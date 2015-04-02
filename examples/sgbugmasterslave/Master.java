/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package sgbugmasterslave;

import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.msg.VM;

public class Master extends Process {
	
	/**
	 * Process to handle one live migration, used to parallelize migrations during off-loads 
	 * @author julien.gossa@unistra.fr
	 */
	protected class LiveMigrationProcess extends Process {
		private VM vm;
		private Host host;
		
		protected LiveMigrationProcess(Host controller, VM vm, Host host) throws HostNotFoundException {
			super(controller, "LiveMigrationProcess:"+vm.getName()+"->"+host.getName());
			this.vm = vm;
			this.host = host;
			try {
				this.start();
			} catch(HostNotFoundException e) {
				Msg.critical("Something bad happend in the LiveMigrationProcess of RICE"+e.getMessage());
			}
		}

		public void main(String[] arg0) throws MsgException {
			Msg.info("Start of lmp : "+this.name);
			vm.migrate(host);
			Msg.info("End of lmp : "+this.name);
		}
	}

	
	public Master(Host host, String name, String[] args) {
		super(host, name, args);
	}

	public void main(String[] args) throws MsgException {

		int nVMs = 10;
		Host [] hosts = Host.all();
		VM [] vms = new VM[nVMs];
		
		for (int i=0; i<nVMs; i++)
		{
			vms[i] = new VM(hosts[0],"vm-"+i, 1, 256, 10,"/default", 1000, 10, 1);
			vms[i].start();
			
			Msg.info("Vm "+vms[i].getName()+" created on "+hosts[0].getName());
		}
		
		// Wait an arbitrary time
		waitFor(1000);
		
		Msg.info("Sequential migrations");
		for (int i=0; i<nVMs; i++)
		{
			Host dest = hosts[1+(i%(hosts.length-1))];
			Msg.info("Migrating vm "+vms[i].getName()+" to "+dest.getName());
			vms[i].migrate(dest);
		}

		// Wait an arbitrary time
		waitFor(1000);

		Msg.info("Parallel migrations");
		for (int i=0; i<nVMs; i++)
		{
			Host dest = hosts[1+(i%(hosts.length-1))];
			Msg.info("Migrating vm "+vms[i].getName()+" to "+dest.getName());
			LiveMigrationProcess lmp = new LiveMigrationProcess(hosts[0],vms[i],dest);
			lmp.start();
		}

		waitFor(1000);
		// Terminating 
		Msg.info("Terminating");
		for (int i = 0; i < nVMs; i++) {
			vms[i].destroy();
		}
		Msg.info("Goodbye now!");

	}
}
