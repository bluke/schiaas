/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package allmasterslave;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.NativeException;
import org.simgrid.schiaas.SchIaaS;

public class Masterslave {
   public static final int TASK_COMP_SIZE = 2000000000;
   public static final int TASK_COMM_SIZE = 10000000;
   
	public static enum VERSION {
		CLOUD, VM, VMHOST, HOST
	};
   public static VERSION version;
   
   /* This only contains the launcher. If you do nothing more than than you can run 
    *   java simgrid.msg.Msg
    * which also contains such a launcher
    */
   
    public static void main(String[] args) throws NativeException, HostNotFoundException {       
	    /* initialize the MSG simulation. Must be done before anything else (even logging). */
	    Msg.init(args);
	
	    if (args.length < 4) {
			Msg.info("Usage   : Masterslave platform_file deployment_file cloud_file");
			Msg.info("example : Masterslave basic_platform.xml basic_deployment.xml basic_cloud.xml");
			System.exit(1);	
		}
	    
	    version = VERSION.valueOf(args[3]);
	    
		/* construct the platform and deploy the application */
		Msg.createEnvironment(args[0]);
		Msg.deployApplication(args[1]);
		
		/* construct the cloud and deploy the associated processes */
		if (version == VERSION.CLOUD) {
			Msg.info("Cloud Initialization");
			SchIaaS.init(args[2]);
		}
		
		/* execute the simulation */
        Msg.run();        
        
        /* print cloud reports */
        /*
        Msg.info("Cloud details\n");
        for (Cloud cloud : SchIaaS.getClouds())  {
        	Msg.info("Cloud:"+cloud.getId());
        	for (Instance instance : cloud.getCompute().describeInstances()) {
        		Msg.info(" - "+instance);
        	}
        }
        */
    }
}