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
import org.simgrid.msg.VM;

import org.simgrid.schiaas.api.*;

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
		
		// Create an image of size 2 GB, 5 GFlo boot process and 1 GFlo shutdown process
		Image myImage = new Image("monimage",2e9,5e9,1e9);

		// Run one instance per slave on myCloud
		Instance[] slaveInstances = Compute.runInstances(myImage,"myCloud",slavesCount);

		
		for (int i=0; i<slavesCount; i++) {
			Msg.info("waiting for boot");
			slaveInstances[i%slavesCount].waitForRunning();

			String [] slaveArgs = {""+i};
			Slave s = new Slave(slaveInstances[i], "slave_"+i,slaveArgs);
			
			s.start();
		}
		
		
		Msg.info("start sending tasks");
		
		for (int i = 0; i < tasksCount; i++) {
			Task task = new Task("Task_" + i, taskComputeSize, taskCommunicateSize); 
			Msg.info("Sending \"" + task.getName()+ "\" to \"slave_" + i % slavesCount + "\"");
			task.send("slave_"+(i%slavesCount));
		}

		
		waitFor(150);
		slaveInstances[0].suspend();
		waitFor(200);
		slaveInstances[0].resume();
		 
		
		Msg.info("All tasks have been dispatched. Let's tell everybody the computation is over.");

		for (int i = 0; i < slavesCount; i++) {
			FinalizeTask task = new FinalizeTask();
			task.send("slave_"+(i%slavesCount));
		}
		

		waitFor(3600);
		Compute.terminate();
		
		Msg.info("Goodbye now!");
	}
}
