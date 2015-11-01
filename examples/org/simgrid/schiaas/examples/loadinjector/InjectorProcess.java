/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package org.simgrid.schiaas.examples.loadinjector;

import java.util.Map.Entry;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.SchIaaS;

public class InjectorProcess extends Process {
	public InjectorProcess(Host host, String name, String[] args) {
		super(host, name, args);
	}

	public void main(String[] args) throws MsgException {
		if (args.length < 1) {
			Msg.info("InjectorProcess needs at least one argument");
			System.exit(1);
		}

		Msg.info("Starting the injector "+args[0]);
		
		// Retrieve the Compute module of MyCloud
		Entry<String, Cloud> entry = SchIaaS.getClouds().entrySet().iterator().next();
		Msg.info("Injecting in the cloud "+entry.getKey());
		Cloud cloud = entry.getValue();
		
		// Initializing the injector
		AbstractInjector injector = null;
		
		try {
			// Creating and running the injector			
			injector = (AbstractInjector)Class.forName(args[0]).getConstructor(Cloud.class, String[].class).newInstance(cloud, args);
			injector.run();
		} catch (Exception e) {
			Msg.critical("Something wrong happened while loading the injector "
					+ args[0]);
			e.printStackTrace();
		}	
		
		// Terminating SchIaaS
		Msg.info("Terminating");
		SchIaaS.terminate();

		Msg.info("Goodbye now!");
	}
}
