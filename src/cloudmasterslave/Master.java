/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package cloudmasterslave;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.process.SchIaaSTask;

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
			Msg.info(instanceType.getId()
					+ ": "
					+ SchIaaS.getCloud("myCloud").getCompute()
							.describeAvailability(instanceType.getId()));
		}

		// Run one instance per slave on myCloud
		String[] slaveInstancesId = myCompute.runInstances("myImage", "small", slavesCount);

		// Check how many instances have been actually run
		slavesCount = myCompute.describeInstances().size();
		Msg.info("Actual available instances count: " + slavesCount);

		for (int i = 0; i < slavesCount; i++) {

			// Wait for the instance to boot
			while (myCompute.describeInstance(slaveInstancesId[i]).isRunning() == 0) {
				waitFor(10);
			}

			// Start Slave process on each running instances
			String[] slaveArgs = { slaveInstancesId[i] /*the name of the mailbox*/ };
			Slave s = new Slave(myCompute.describeInstance(slaveInstancesId[i]),
								slaveInstancesId[i], slaveArgs);
			s.start();
		}

		/**
		 * message management
		 */
		
		Msg.info("start sending tasks");

		for (int i = 0; i < tasksCount; i++) {
			Task task = new SchIaaSTask("Task_" + i, taskComputeSize,
					taskCommunicateSize, SchIaaSTask.TYPE.JOB);
			Msg.info("Sending \"" + task.getName() + "\" to "
					+ slaveInstancesId[i % slavesCount]);
			// task.send("slave_"+(i%slavesCount));
			SchIaaS.getCloud("myCloud").getNetwork()
					.sendTask(task, slaveInstancesId[i % slavesCount]);

		}

		/**
		 * storage management (including storage cost)
		 */
		
		// Move some data inside the storage device
		Task data = new SchIaaSTask("Task_data", taskComputeSize,
				2028 * taskCommunicateSize, SchIaaSTask.TYPE.DATA);
		Msg.info("Storing data \"" + data.getName() + "\" to storage: "
				+ SchIaaS.getCloud("myCloud").getStorage("s3").getId());
		SchIaaS.getCloud("myCloud").getStorage("s3").moveDataIn(data);

		// Do some requests on the storage
		Task request = new SchIaaSTask("Task_data", taskComputeSize,
				taskCommunicateSize, SchIaaSTask.TYPE.REQUEST);
		Msg.info("Performing requests on storage \""
				+ SchIaaS.getCloud("myCloud").getStorage("s3").getId());
		SchIaaS.getCloud("myCloud").getStorage("s3").makeRequest(request);

		// Check cost for storing the data
		Msg.info("Current storage cost: "
				+ SchIaaS.getCloud("myCloud").getStorage("s3").getCurrentStoredDataCost());
		
		// Move the data out of the storage device
		Msg.info("Getting data \"" + data.getName() + "\" from storage: "
				+ SchIaaS.getCloud("myCloud").getStorage("s3").getId());
		SchIaaS.getCloud("myCloud").getStorage("s3").moveDataOut(data);
		
		

		/**
		 * more VM instance management
		 */
		
		// Suspend and resume one instance
		waitFor(150);
		Msg.info("Suspending " + slaveInstancesId[0]);
		SchIaaS.getCloud("myCloud").getCompute()
				.suspendInstance(slaveInstancesId[0]);
		waitFor(200);
		Msg.info("Resuming " + slaveInstancesId[0]);
		SchIaaS.getCloud("myCloud").getCompute()
				.resumeInstance(slaveInstancesId[0]);

		Msg.info("All tasks have been dispatched. Let's tell everybody the computation is over.");

		for (int i = 0; i < slavesCount; i++) {
			FinalizeTask task = new FinalizeTask();
			// task.send(slaveInstancesId[i % slavesCount]);
			SchIaaS.getCloud("myCloud").getNetwork()
					.sendTask(task, slaveInstancesId[i % slavesCount]);
		}

		// Wait an arbitrary time for Slaves to finalize
		waitFor(3600);
		// Terminating SchIaaS
		Msg.info("Terminating");
		SchIaaS.terminate();
		Msg.info("Goodbye now!");

		/**
		 * billing management (except storage costs - see above) 
		 */
		
		// Billing
		Msg.info("Total cost for running the VMs: "
				+ SchIaaS.getCloud("myCloud").getCompute().getCost());
		Msg.info("Total cost for transfering data for cloud: "
				+ SchIaaS.getCloud("myCloud").getId() + " : "
				+ SchIaaS.getCloud("myCloud").getNetwork().getTransferCost());

		Msg.info("Total storage transfer cost: "
				+ SchIaaS.getCloud("myCloud").getStorage("s3").getStorageTransferCost());

	}
}
