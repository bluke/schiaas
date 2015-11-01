/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package org.simgrid.schiaas.examples.loadinjector;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.NativeException;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.tracing.Trace;

public class Injector {   
   
    public static void main(String[] args) throws NativeException, HostNotFoundException {       
	    /* initialize the MSG simulation. Must be done before anything else (even logging). */
	    Msg.init(args);
	
	    if (args.length < 3) {
			Msg.info("Usage   : Injector platform_file deployment_file cloud_file");
			Msg.info("example : Injector basic_platform.xml basic_deployment.xml basic_cloud.xml");
			System.exit(1);	
		}
	    
		/* construct the platform and deploy the application */
		Trace.init("injector.trace", "Example of LoadInjector on cloud");
		Msg.createEnvironment(args[0]);
		Msg.deployApplication(args[1]);
		SchIaaS.init(args[2]);
		
		/* execute the simulation */
        Msg.run();        
        
        /* end the simulation */
        Trace.close();
    }
}