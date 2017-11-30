/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package loadinjector;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.loadinjector.Injector;
import org.simgrid.schiaas.tools.Trace;

public class SimpleInjection {   
   
    public static void main(String[] args) throws MsgException, HostNotFoundException {       
	    /* initialize the MSG simulation. Must be done before anything else (even logging). */
	    Msg.init(args);
	
	    if (args.length < 4) {
			Msg.info("Usage   : Injector platform_file deployment_file cloud_file injector_file");
			Msg.info("example : Injector platform.xml deploy.xml cloud.xml injector.xml");
			System.exit(1);	
		}
	    
		/* construct the platform and deploy the application */
		Trace.init("Example of LoadInjector on cloud");
		Msg.createEnvironment(args[0]);
		SchIaaS.init(args[2]);
		Injector.init(args[3]);
		InjectionScheduler.Init();
		
		Msg.deployApplication(args[1]);
		
		/* execute the simulation */
        Msg.run(); 
        
        /* end the simulation */
        Trace.close();
    }
}