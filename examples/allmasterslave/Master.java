/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package allmasterslave;

import java.util.Collection;
import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.SchIaaS;

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
		Host [] hosts = new Host[slavesCount];
		VM [] vms = new VM[slavesCount];
		int nHosts = Host.all().length;
		
		switch(Masterslave.version) {
		case CLOUD:
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
			Collection<Instance> slaveInstances = myCompute.runInstances("myImage", "small", slavesCount);
			
			
			// Wait for the instances to boot
			int h = 0;
			for (Instance instance : slaveInstances) {
				Msg.info("waiting for boot");
				while (instance.isRunning() == 0) {
					waitFor(10);
				}

				hosts[h++]=instance.vm();
			}
			break;

		case VM:
			for (int i=0; i<slavesCount; i++) {
				Msg.info("waiting for boot "+i+" "+nHosts+" node-"+(i%(nHosts-1)+1)+".me");
				VM vm = new VM(Host.getByName("node-"+(i%(nHosts-1)+1)+".me"),
						"vm-"+i, 1, 256, 10,"/default", 1000, 10, 1);
				vm.start();
				hosts[i]=vm;
			}
			break;
		
		case VMHOST:	
			for (int i=0; i<slavesCount; i++) {
				Msg.info("waiting for boot "+i+" "+nHosts+" node-"+(i%(nHosts-1)+1)+".me");
				VM vm = new VM(Host.getByName("node-"+(i%(nHosts-1)+1)+".me"),
						"vm-"+i, 1, 256, 10,"/default", 1000, 10, 1);
				vm.start();
				vms[i]=vm;
				hosts[i]=Host.getByName("node-"+(i%(nHosts-1)+1)+".me");
			}
			break;
			
		case HOST:
			for (int i=0; i<slavesCount; i++) {
				Msg.info("waiting for boot "+i+" node-"+(i%(Host.all().length-1)+1)+".me");
	
				hosts[i]=Host.getByName("node-"+(i%(Host.all().length-1)+1)+".me");
			}
			break;
	
			
		}
		
		for (int i=0; i<slavesCount; i++) {
			Msg.info("Starting a slave on "+hosts[i].getName());
			String [] slaveArgs = {""+i};
			Slave s = new Slave(hosts[i], "slave_"+i,slaveArgs);
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

		Msg.info("All tasks have been dispatched. Let's tell everybody the computation is over.");

		for (int i = 0; i < slavesCount; i++) {
			FinalizeTask task = new FinalizeTask();
			task.send("slave_"+(i%slavesCount));
		}

		// Wait an arbitrary time for Slaves to finalize
		waitFor(36000);
		// Terminating SchIaaS
		Msg.info("Terminating");
		switch(Masterslave.version) {
		case CLOUD:
			SchIaaS.terminate();
			break;
		case VM:
			for (int i = 0; i < slavesCount; i++) {
				VM vm = (VM) hosts[i];
				vm.shutdown();
			}
			break;			
		case VMHOST:
			for (int i = 0; i < slavesCount; i++) {
				vms[i].shutdown();
			}
			break;
		case HOST:
			break;
		}
		Msg.info("Goodbye now!");

	}
}
