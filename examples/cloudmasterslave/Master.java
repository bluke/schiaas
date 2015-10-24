/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package cloudmasterslave;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeHost;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.engine.compute.rice.RiceInstance;

public class Master extends Process {
	public Master(Host host, String name, String[] args) {
		super(host, name, args);
	}

	public void main(String[] args) throws MsgException {
		if (args.length < 4) {
			Msg.info("Master needs 4 arguments");
			System.exit(1);
		}

		int tasksCount = Integer.valueOf(args[0]).intValue();
		double taskComputeSize = Double.valueOf(args[1]).doubleValue();
		double taskCommunicateSize = Double.valueOf(args[2]).doubleValue();

		int slavesCount = Integer.valueOf(args[3]).intValue();

		Msg.info("Hello! Got " + slavesCount + " slaves and " + tasksCount
				+ " tasks to process");
		
		
		/*********************** CLOUD USER INTERFACE ***********************/
		
		/**
		 * VM instance management
		 */
		// Retrieve the Compute module of MyCloud
		Compute compute = SchIaaS.getCloud("myCloud").getCompute();
		
		// Check the availability of resources on MyCloud
		Msg.info("Instances availability:");
		for (InstanceType instanceType : compute.describeInstanceTypes()) {
			Msg.info(instanceType.getId()+ ": "
					+ compute.describeAvailability(instanceType.getId()));
		}
		
		// Run one instance per slave on myCloud
		Collection<Instance> instances = compute.runInstances("myImage", "small", slavesCount);
  
		// Check how many instances have been accepted and retrieve them into an array
		// (One can actually use the Collection "instances", that's just to fit the legacy simgrid syntax)
		slavesCount = compute.describeInstances().size();
		Instance[] slaveInstances = instances.toArray(new Instance[slavesCount]);

		Msg.info("running slaves "+slavesCount);
		
		for (int i=0; i<slavesCount; i++) {
			Msg.info("waiting for boot");
			while (slaveInstances[i].isRunning() == 0) {
				waitFor(10);
			}
			
			Msg.info("Starting a slave on "+slaveInstances[i].getName());
			String [] slaveArgs = {""+i};
			Slave s = new Slave(slaveInstances[i], "slave_"+i,slaveArgs);
			s.start();
		}
		
		/**
		 * message management
		 */
		Msg.info("start sending tasks");

		for (int i = 0; i < tasksCount; i++) {
			Task task = new Task("Task_" + i, taskComputeSize, taskCommunicateSize);
			Msg.info("Sending \"" + task.getName() + "\" to slave_"+(i % slavesCount));

			task.send("slave_"+(i%slavesCount));
			//SchIaaS.getCloud("myCloud").getNetwork().sendTask(task, slaveInstancesId[i % slavesCount]);
		}
		
		/**
		 * storage management (including storage cost)
		 */
		Storage myStorage = SchIaaS.getCloud("myCloud").getStorage("myStorage");
		
		Data someData = new Data("someData", 1e9);

		Msg.info("Store some data");
		myStorage.put(someData);
				
		Msg.info("Retrieve some stored data");
		someData = myStorage.get("someData");

		Msg.info("Retrieve some data that has not been stored");
		Data unstoredData = new Data("unstoredData", 2e9);
		someData = myStorage.get(unstoredData);
		
		Msg.info("List the stored data");
		Map <String, Data> storedData = myStorage.list(); 	//1st way
		myStorage.ls();										//2d way
		
		Msg.info("Delete some data");
		myStorage.delete("someData");
		myStorage.ls();
	

		/**
		 * Suspend and resume
		 */

		waitFor(150);
		Msg.info("Suspending " + slaveInstances[0].getId());
		Msg.info(" - isRunning = " + slaveInstances[0].isRunning() );
		compute.suspendInstance(slaveInstances[0].getId());
		waitFor(1);
		Msg.info(" - isRunning = " + slaveInstances[0].isRunning() );
		waitFor(200);
		Msg.info(" - isRunning = " + slaveInstances[0].isRunning() );
		Msg.info("Resuming " + slaveInstances[0].getId());
		compute.resumeInstance(slaveInstances[0].getId());
		waitFor(1);
		Msg.info(" - isRunning = " + slaveInstances[0].isRunning() );
		waitFor(100);
		
		
		
		/*********************** CLOUD ADMIN INTERFACE ***********************/

		/** 
		 * Retrieving :
		 * - The engine, which is the main admin interface
		 * - The list of physical host of the cloud
		 * - One of its host 
		 * - The instances hosted on one host
		 * - The ComputeHost hosting one instance
		 */ 
		ComputeEngine computeEngine = compute.getComputeEngine();
		List<ComputeHost> computeHosts = computeEngine.getComputeHosts();
		ComputeHost computeHost1 = computeHosts.get(0);
		Collection<Instance> instances1 = computeHost1.getHostedInstances();
		ComputeHost computeHost = computeEngine.getComputeHostOf(instances1.iterator().next());

		/**
		 * Migrating one VM from the second host to the first
		 */
		ComputeHost computeHost2 = null;
		try {
			computeHost2  = computeHosts.get(1);
		} catch (Exception e) {
			Msg.critical("This scenario needs at least two hosts in the cloud compute");
			e.printStackTrace();
		}
		
		try {
			Instance instance = computeHost2.getHostedInstances().iterator().next();
			Msg.info("Migrating "+instance.getId()+" to "+ computeHost1.getHost().getName());
			Msg.info(" - current host: " + computeEngine.getComputeHostOf(instance).getHost().getName());
			computeEngine.liveMigration(instance, computeHost1);
			Msg.info("Migration of "+instance.getId()+" to "+computeHost1.getHost().getName()+" complete.");
			Msg.info(" - current host: " + computeEngine.getComputeHostOf(instance).getHost().getName());
		} catch (Exception e) {
			Msg.info("Second Host not found, or no instance hosted on it.");
		}
		

		/**
		 * Offloading the first host of all of it VMs sequentially
		 */
		Msg.info("Sequential offloading of "+computeHost1.getHost().getName());
		Msg.info(" - Number of hosted instances  "+ computeHost1.getHostedInstances().size());
		ComputeTools.offLoad(computeEngine, computeHost1);
		Msg.info("Offloading "+computeHost1.getHost().getName()+" complete.");
		Msg.info(" - Number of hosted instances  "+ computeHost1.getHostedInstances().size());
		
		/**
		 * Resetting the availability of the first computeHost to host new VMs.
		 */
		computeHost1.setAvailability(true);
		
		/**
		 * offloading the second host of all of it VMs in parallel 
		 */
		Msg.info("Parallel offloading of "+computeHost2.getHost().getName() + " (might take some time due to network bottleneck)");
		Msg.info(" - Number of hosted instances  "+ computeHost2.getHostedInstances().size());
		ComputeTools.parallelOffLoad(computeEngine, computeHost2);
		int j = 0;
		while (computeHost2.getHostedInstances().size() != 0) {
			Msg.info("Still " + computeHost2.getHostedInstances().size() + " instances running on " + computeHost1 );
			waitFor(100);
		}
		Msg.info("Offloading "+computeHost2.getHost().getName()+" complete.");
		Msg.info(" - Number of hosted instances  "+ computeHost2.getHostedInstances().size());
		
		/*********************** END OF SIMULATION ***********************/
		
		Msg.info("All tasks have been dispatched. Let's tell everybody the computation is over.");

		for (int i = 0; i < slavesCount; i++) {
			FinalizeTask task = new FinalizeTask();
			task.send("slave_"+(i%slavesCount));
		}

		// Wait an arbitrary time for Slaves to finalize
		waitFor(36000);
		
		compute.terminateInstance(slaveInstances[0].getId());
		
		// Terminating SchIaaS
		Msg.info("Terminating");
		SchIaaS.terminate();

		Msg.info("Goodbye now!");
	}
}
