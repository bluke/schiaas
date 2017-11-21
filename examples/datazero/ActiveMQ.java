/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package datazero;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeHost;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

public class ActiveMQ extends Process {
	
	/**
	 * Enumerates the possible commands.
	 */
	protected static enum COMMANDS {
		START, STOP, INFO, QUIT;
	};
	
	public ActiveMQ(Host host) {
		super(host, "ActiveMQ", null);
	}
	
	public void main(String[] args) throws MsgException {
		
		Msg.info("ActiveMQ is ready.");
		
		COMMANDS command;
		Compute compute = SchIaaS.getCloud("myCloud").getCompute();
		long date1, delay;
		
		Instance instance = null;
		
		do {
			date1 = System.currentTimeMillis();
			command = COMMANDS.valueOf(System.console().readLine());
			delay = (System.currentTimeMillis()-date1)/1000;
			
			Msg.info("Synchronizing simulation with reality.");
			waitFor(delay);
			Msg.info("Synchronized.");
			
			switch (command) {
			
			case START:
				try {
					instance = compute.runInstance("myImage", "small");
					DataZeroProcess dzp = new DataZeroProcess(instance.vm());
					dzp.start();
				} catch (VMSchedulingException e) {
					Msg.info("Can not run another VM.");
				}
				Msg.info("Started "+instance);
				break;
				
			case STOP :
				instance = (Instance) compute.describeInstances().toArray()[0];
				instance.terminate();
				Msg.info("Stopped "+instance);
				break;
				
			case INFO :
				for(Instance instance2 : compute.describeInstances())  {
					Msg.info(" - "+instance2);
				}
		}
			
		} while(command != COMMANDS.QUIT);
		
		
		// Terminate a given instance
		//compute.terminateInstance(slaveInstances[0]);
		
		// Terminating SchIaaS (and all instances at once)
		Msg.info("Terminating");
		//SchIaaS.terminate();
		
		Msg.info("Goodbye now!");
	}
}
