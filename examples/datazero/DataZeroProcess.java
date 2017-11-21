/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */
package datazero;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;

public class DataZeroProcess extends Process {
	public DataZeroProcess(Host host) {
		super(host,"DZP",null);
	}
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
		if (args.length < 0) {
			System.exit(1);
		}
		
		Msg.info("DataZeroProcess ran.");
		
		while(true) { 
			Msg.info("DataZeroProcess execute");
			waitFor(1);
		}

	}
}
