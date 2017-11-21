/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package datazero;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.tools.Trace;

public class Datazero {
   
    public static void main(String[] args) throws MsgException, HostNotFoundException {       
	    /* initialize the MSG simulation. Must be done before anything else (even logging). */
	    Msg.init(args);
	
	    if (args.length < 2) {
			Msg.info("Usage   : Datazero platform_file cloud_file");
			Msg.info("example : Datazero basic_platform.xml basic_cloud.xml");
			System.exit(1);	
		}
	    
		/* construct the platform and deploy the application */
		Msg.createEnvironment(args[0]);
		
		/* construct the cloud and deploy the associated processes */
		Msg.info("Cloud Initialization");
		Trace.init("Example of MasterSlavec on cloud");
		
		SchIaaS.init(args[1]);
		
		/* deploy ActiveMQ process */
		ActiveMQ activeMQ = new ActiveMQ(Host.getByName("SupervisionHost"));
		activeMQ.start();
		
		/* execute the simulation */
        Msg.run();
        
        /* print cloud reports */
        Msg.info("Cloud details\n");
        for (Cloud cloud : SchIaaS.getClouds().values())  {
        	Msg.info(""+cloud);
        	for (Instance instance : cloud.getCompute().describeInstances()) {
        		Msg.info(" - "+instance);
        	}
        }
        
        Trace.close();
    }
}