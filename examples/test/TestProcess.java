/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package test;


import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.VM;

public class TestProcess extends Process {
	public TestProcess(Host host, String name, String[] args) {
		super(host, name, args);
	}

	public void main(String[] args) throws MsgException {
		
		Msg.info("Test : "+Host.all().length);
		
		Host pm1 = Host.getByName("node-1.me");
		Host pm2 = Host.getByName("node-2.me");

		VM vm1 = new VM(pm1, "VM1", 250, 250, 1);
		VM vm2 = new VM(pm1, "VM2", 250, 250, 1);
		
		vm1.start();
		vm2.start();
		
		vm1.setBound(0.20e9);
		Burn burn1 = new Burn(vm1, "P1", 1e9);
		Burn burn2 = new Burn(vm2, "P2", 1e9);
		
		burn1.start();
		burn2.start();

		waitFor(10000);

		vm1.shutdown();
		vm2.shutdown();

		vm1 = null;
		vm2 = null;
				
		Msg.info("Goodbye now!");
	}
}
