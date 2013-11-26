/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package cloudmasterslave;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.NativeException;
import org.simgrid.msg.Process;
import org.simgrid.simiaas.api.Compute;
import org.simgrid.simiaas.api.Instance;

public class Masterslave {
   public static final int TASK_COMP_SIZE = 2000000000;
   public static final int TASK_COMM_SIZE = 10000000;
   /* This only contains the launcher. If you do nothing more than than you can run 
    *   java simgrid.msg.Msg
    * which also contains such a launcher
    */
   
    public static void main(String[] args) throws NativeException, HostNotFoundException {       
	    /* initialize the MSG simulation. Must be done before anything else (even logging). */
	    Msg.init(args);
	
	    if (args.length < 3) {
			Msg.info("Usage   : Masterslave platform_file deployment_file cloud_file");
			Msg.info("example : Masterslave basic_platform.xml basic_deployment.xml basic_cloud.xml");
			System.exit(1);	
		}
		/* construct the platform and deploy the application */
		Msg.createEnvironment(args[0]);
		Msg.deployApplication(args[1]);
		
		/* construct the cloud and deploy the associated processes */
		Compute.init(args[2]);

		/* execute the simulation */
        Msg.run();
        
        /* print cloud reports */
        Msg.info("Cloud description\n"+Compute.getCloudsDescription());
        Msg.info("Instances description\n"+Compute.getInstancesDescription());
        Msg.info("Details\n");
        for (Instance i : Compute.describeInstances())  {
        	Msg.info(i.stateLogToString());
        }
    }
}
