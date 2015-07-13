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
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.Storage;

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

		
		
		
		/**
		 * VM instance management
		 */
		// Retrieve the Compute module of MyCloud
		Compute myCompute = SchIaaS.getCloud("myCloud").getCompute();
		
		// Check the availability of resources on MyCloud
		Msg.info("Instances availability:");
		for (InstanceType instanceType : myCompute.describeInstanceTypes()) {
			Msg.info(instanceType.getId()+ ": "
					+ myCompute.describeAvailability(instanceType.getId()));
		}
		
		
		// Run one instance per slave on myCloud
		String[] slaveInstancesId = myCompute.runInstances("myImage", "small", slavesCount);

		

		for (int i=0; i<slavesCount; i++) {
			Msg.info("waiting for boot");
			while (myCompute.describeInstance(slaveInstancesId[i]).isRunning() == 0) {
				waitFor(10);
			}
			
			Msg.info("Starting a slave on "+myCompute.describeInstance(slaveInstancesId[i]).getName());
			String [] slaveArgs = {""+i};
			Slave s = new Slave(myCompute.describeInstance(slaveInstancesId[i]),
								"slave_"+i,slaveArgs);
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
		
		Msg.info("Check whether the data transfer is complete: " +
				myStorage.isTransferComplete("someData"));
		
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
		 * more VM instance management
		 */

		// Suspend and resume one instance
		waitFor(150);
		Msg.info("Suspending " + slaveInstancesId[0]);
		Msg.info(" - "+myCompute.describeInstance(slaveInstancesId[0]));
		myCompute.suspendInstance(slaveInstancesId[0]);
		Msg.info(" - "+myCompute.describeInstance(slaveInstancesId[0]));
		waitFor(200);
		Msg.info(" - "+myCompute.describeInstance(slaveInstancesId[0]));
		Msg.info("Resuming " + slaveInstancesId[0]);
		Msg.info(" - "+myCompute.describeInstance(slaveInstancesId[0]));
		myCompute.resumeInstance(slaveInstancesId[0]);
		waitFor(100);
		
		// Migrate one VM, as this is an admin operation, one must use the compute engine
		Collection<Host> hosts = myCompute.getComputeEngine().getHosts();
		Msg.info("Migrating "+slaveInstancesId[0]+" to "+(Host)hosts.toArray()[1]);
		Msg.info(" - " + myCompute.describeInstance(slaveInstancesId[0]));
		myCompute.getComputeEngine().liveMigration(slaveInstancesId[0], (Host)hosts.toArray()[1]);
		Msg.info("Migration of "+slaveInstancesId[0]+" to "+(Host)hosts.toArray()[1]+" complete.");
		Msg.info(" - " + myCompute.describeInstance(slaveInstancesId[0]));
		

		// Offload one physical host
		Msg.info("Offloading "+(Host)hosts.toArray()[1]);
		for (int i=0; i<slavesCount; i++) 
			Msg.info(" - " + myCompute.describeInstance(slaveInstancesId[i]));
		myCompute.getComputeEngine().offLoad((Host)hosts.toArray()[1]);
		Msg.info("Offloading "+(Host)hosts.toArray()[1]+" complete.");
		for (int i=0; i<slavesCount; i++) 
			Msg.info(" - " + myCompute.describeInstance(slaveInstancesId[i]));
		
		
		
		Msg.info("All tasks have been dispatched. Let's tell everybody the computation is over.");

		for (int i = 0; i < slavesCount; i++) {
			FinalizeTask task = new FinalizeTask();
			task.send("slave_"+(i%slavesCount));
			//SchIaaS.getCloud("myCloud").getNetwork().sendTask(task, slaveInstancesId[i % slavesCount]);
		}

		// Wait an arbitrary time for Slaves to finalize
		waitFor(36000);
		
		myCompute.terminateInstance(slaveInstancesId[0]);
		
		// Terminating SchIaaS
		Msg.info("Terminating");
		SchIaaS.terminate();

		Msg.info("Goodbye now!");
	}
}
