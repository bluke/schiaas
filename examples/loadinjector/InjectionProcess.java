/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package loadinjector;


import java.util.Iterator;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.loadinjector.AbstractInjector;
import org.simgrid.schiaas.loadinjector.Injector;

public class InjectionProcess extends Process {
	
	protected AbstractInjector injector;
	
	public InjectionProcess(Host host, String name, String[] args) {
		
		super(host, name, args);
		Msg.info("injector "+Injector.getInjectors().size());
		
		// Picking the first injector
		Iterator<AbstractInjector> iterator = Injector.getInjectors().values().iterator();
		this.injector = iterator.next();
		iterator.remove();
		
		if (iterator.hasNext()) {
			InjectionProcess ip = new InjectionProcess(host, name, args);
			try {
				ip.start();
			} catch (HostNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void main(String[] args) throws MsgException {

		
		// running this injector
		injector.run();
		
		// Terminating SchIaaS (and all instances at once)
		Msg.info("Terminating");
		//SchIaaS.terminate();

		
		Msg.info("Goodbye now!");
	}
}
