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
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.SchIaaS;

public class Master extends Process {
	public Master(Host host, String name, String[]args) {
		super(host,name,args);
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

		Msg.info("Hello! Got "+  slavesCount + " slaves and "+tasksCount+" tasks to process");

		Msg.info("Instances availability:");
		for (InstanceType instanceType : SchIaaS.getCloud("myCloud").getCompute().describeInstanceTypes()) {
			Msg.info(instanceType.getId()+": "+SchIaaS.getCloud("myCloud").getCompute().describeAvailability(instanceType.getId()));
		}
		
		// Run one instance per slave on myCloud
		String[] slaveInstancesId = SchIaaS.getCloud("myCloud").getCompute().runInstances("myImage", "medium", slavesCount);

		// Check how many instances have been actually run
		slavesCount = SchIaaS.getCloud("myCloud").getCompute().describeInstances().size();
		Msg.info("Actual available instances count: "+slavesCount);
		
		for (int i=0; i<slavesCount; i++) {
			
			// Wait for the instance to boot
			while (SchIaaS.getCloud("myCloud").getCompute().describeInstance(slaveInstancesId[i]).isRunning() == 0) {
				waitFor(10);
			}

			// Start Slave process on each running instances			
			String [] slaveArgs = {""+i};
			Slave s = new Slave(SchIaaS.getCloud("myCloud").getCompute().describeInstance(slaveInstancesId[i]), "slave_"+i,slaveArgs);
			
			s.start();
		}
		
		
		Msg.info("start sending tasks");
		
		for (int i = 0; i < tasksCount; i++) {
			Task task = new Task("Task_" + i, taskComputeSize, taskCommunicateSize); 
			Msg.info("Sending \"" + task.getName()+ "\" to \"slave_" + i % slavesCount + "\"");
			task.send("slave_"+(i%slavesCount));
		}

	
		// Suspend and resume one instance : Not working for unknown reason, probably at VM level 
		waitFor(150);
		Msg.info("Suspending "+slaveInstancesId[0]);
		SchIaaS.getCloud("myCloud").getCompute().suspendInstance(slaveInstancesId[0]);
		waitFor(200);
		Msg.info("Resuming "+slaveInstancesId[0]);
		SchIaaS.getCloud("myCloud").getCompute().resumeInstance(slaveInstancesId[0]);

		Msg.info("All tasks have been dispatched. Let's tell everybody the computation is over.");

		for (int i = 0; i < slavesCount; i++) {
			FinalizeTask task = new FinalizeTask();
			task.send("slave_"+(i%slavesCount));
		}

		
		// Wait an arbitrary time for Slaves to finalize 
		waitFor(3600);
		
		// Terminating SchIaaS
		Msg.info("Terminating");
		SchIaaS.terminate();
		Msg.info("Goodbye now!");
	}
}
