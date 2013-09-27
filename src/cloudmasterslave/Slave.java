/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */
package cloudmasterslave;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.process.SchIaaSProcess;

public class Slave extends SchIaaSProcess {

	public Slave(Host host, String name, String[]args) {
		super(host,name,args);	
	}
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
		if (args.length < 1) {
			Msg.info("Slave needs 1 argument (its number)");
			System.exit(1);
		}

		Msg.info("Receiving on slave " + this.instanceId);
		
		while(true) { 
			
			Task task = Task.receive(args[0]);			
			
			double runDate = Msg.getClock();
			if (task instanceof FinalizeTask) {
				break;
			}
			Msg.info("Received \"" + task.getName() +  "\". Processing it.");
			try {
				task.execute();
			} catch (TaskCancelledException e) {

			}
			Msg.info("\"" + task.getName() + "\" done ");
			
			Msg.info("RunTime : "+(Msg.getClock()-runDate));
		}

		Msg.info("Received Finalize. I'm done. See you!");
	}
}
