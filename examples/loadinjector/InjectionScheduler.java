/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package loadinjector;

import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Mutex;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.loadinjector.AbstractInjector;
import org.simgrid.schiaas.loadinjector.Injector;

/**
 * This process run several injectors and wait for their ends.
 * @author julien.gossa@unistra.fr
 */
public class InjectionScheduler extends Process {
	
	private static Host controller;
	private Vector<InjectionProcess> injectionProcesses;
	
	/**
	 * This process run one injector, protected by one mutex
	 * @author julien.gossa@unistra.fr
	 */
	public class InjectionProcess extends Process {		
		protected AbstractInjector injector;
		protected Mutex mutex;

		public InjectionProcess(AbstractInjector injector) {
			super(controller, "InjectionProcess"+injector.getId(), null);
			this.injector = injector;
			this.mutex = new Mutex();
		}

		public void main(String[] args) throws MsgException {
			this.mutex.acquire();
			injector.run();
			this.mutex.release();
		}
	}
	
	/**
	 * Load the injectors and create the associated process 
	 * @param host an host to run this process.
	 */
	public InjectionScheduler(Host host) {
		super(host, "InjectionScheduler", null);
		
		this.injectionProcesses = new Vector<InjectionProcess>();
		
		for (AbstractInjector injector : Injector.getInjectors().values()){
			InjectionProcess ip = new InjectionProcess(injector);
			this.injectionProcesses.add(ip);
			ip.start();
		}
	}

	/**
	 * Start the injectors and wait for their ends.
	 */
	public void main(String[] args) throws MsgException {
		
		for (InjectionProcess ip : injectionProcesses) {
			Msg.info("Starting the injection process of "+ip.injector.getId());
			ip.start();
		}

		// waiting for the injectors to finish
		for (InjectionProcess ip : injectionProcesses) {
			Msg.info("Waiting for "+ip.injector.getId()+" to finish.");
			ip.mutex.acquire();
			ip.mutex.release();
		}
		
		// Terminating SchIaaS (and all instances at once)
		Msg.info("All injections finished. Terminating now.");
		SchIaaS.terminate();

		Msg.info("Goodbye now!");
	}
	
	/**
	 * Initialize the injection scheduler
	 */
	public static void Init() {
		// Try to find a host names "controller"
		// Use the first found host otherwise
		try {
			controller = Host.getByName("controller");
		} catch (HostNotFoundException e) {
			controller = Host.all()[0];
		} 
		Msg.info("Using '"+controller.getName()+"' to host the injection processes" );
		
		InjectionScheduler is = new InjectionScheduler(controller);
		is.start();
	}
}
