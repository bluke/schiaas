/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */
package test;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;

public class Burn extends Process {
	public double bound;
	public Burn(Host host, String name, double bound) {
		super(host,name);
		this.bound = bound;
	}
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
		Task task = new Task("Task_"+this.getName(), 100e9, 0);
		try {
			Msg.info("Start");
			double startDate = Msg.getClock();
			task.setBound(this.bound);
			task.execute();
			Msg.info("End. Duration = "+(Msg.getClock()-startDate));
		} catch (TaskCancelledException e) {
			e.printStackTrace();
		}
	}
}
