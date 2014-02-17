/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package simschlouder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.NativeException;

import simschlouder.algorithms.AStrategy;

public class SimSchlouder {

	public static double standardPower;
	
	public static double time2flops(double time) {
		return time*standardPower;
	}
	
	public static enum StorageType {CLOUD, CLIENT, INSTANCE};
	public static StorageType storageType;
	
	public static class TaskFileReaderProcess extends  org.simgrid.msg.Process {
		
		public TaskFileReaderProcess(Host host, String name, String[] args) {
			super(host,name,args);
		}
		
		// TODO: Errors
		public void readTaskFile(String fileName) throws FileNotFoundException, HostFailureException {
			Scanner scf = new Scanner(new File(fileName));
			HashMap<String, SchloudTask> taskMap = new HashMap<String, SchloudTask>();
			
			double oldSubmissionDate = 0;
			
			while (scf.hasNext()) {
				Scanner sc = new Scanner(scf.nextLine());
				
				String jid = sc.next();
				double submissionDate = sc.nextDouble();
				
				if (submissionDate != oldSubmissionDate) {
					Msg.verb("Waiting for next bag of tasks");
					waitFor(submissionDate-oldSubmissionDate);
				}
				
				double runtime = sc.nextDouble();
				
				double inputSize = 0;
				double outputSize = 0;
				if (sc.hasNextDouble()) {
					inputSize = sc.nextDouble()*1e6;
					outputSize = sc.nextDouble()*1e6;
				}
				
				Vector<SchloudTask> dependencies = new Vector<SchloudTask>();
				if (sc.hasNext("\\-\\>")) {
					sc.next("\\-\\>");
					while (sc.hasNext()) {
						dependencies.add(taskMap.get(sc.next()));
					}
				}

				//Msg.verb(Dependencies:" : "+dependencies.size());
				SchloudTask task = new SchloudTask(jid, runtime, inputSize, outputSize, dependencies); 
				taskMap.put(task.name, task);
				
				Msg.verb("Enqueuing " + task);
				SchloudController.enqueueTask(task);
				sc.close();
				
				oldSubmissionDate=submissionDate;
			}
			scf.close();
			SchloudController.allTasksSubmitted = true;
		}

		@Override
		public void main(String[] args) throws MsgException {
			try {
				readTaskFile(args[0]);
			} catch (FileNotFoundException e) {
				Msg.critical("Task file "+args[0]+" not found.");
				e.printStackTrace();
			}
		}
	}
	
    public static void main(String[] args) throws NativeException, HostNotFoundException, IOException, HostFailureException {       
	    Msg.init(args);
	
	    if (args.length < 3) {
			Msg.info("Usage   : SimSchlouder simschlouder_file tasks_file strategyClass");
			Msg.info("example : SimSchlouder simschlouder.xml workload.tasks ASAP");
			System.exit(1);	
		}
		
	    
		SchloudController.init(args[0]);
		
		Msg.verb("Reading the task file: "+args[1]);
		String[] tfrpargs = {args[1]};
		TaskFileReaderProcess tfrp = new TaskFileReaderProcess(Host.getByName("client"),"TaskFileReader",tfrpargs);
		tfrp.start();
		
		Msg.verb("Loading the strategy: "+args[2]);
		//SchloudController.strategy=SchloudController.STRATEGY.valueOf(args[2].toUpperCase());
		SchloudController.strategy = SimSchlouder.loadStrategy(args[2].trim());
		Msg.info("Strategy set to "+SchloudController.strategy.getName());
		
		
		Msg.verb("Running the simulation...");
		/*  execute the simulation. */
        Msg.run();
        
        //Msg.info("Cloud description\n"+Compute.getCloudsDescription());
        //Msg.info("Nodes description\n"+SchloudController.getPostMortem());
        //Msg.info("Instances description\n"+Compute.getInstancesDescription());
        System.out.println("Outcomes:\n"+SchloudController.getOutcome());
        
        SchloudController.writeJSON("simschlouder");
    }
    
    
    
    /**
     * Load a scheduling and provisioning strategy class 
     * @param alg the name of the class
     * @return an <i>AStrategy</i> object
     */
	private static AStrategy loadStrategy(String alg) {
    	try {
    		return (AStrategy)Class.forName("simschlouder.algorithms."+alg).newInstance();
    	} catch (ClassNotFoundException ex) {
    		Msg.error("Could not locate class: " + alg);
    		System.exit(1);
    	} 
		catch (Exception e) {
			Msg.error("Unexpected exception when instanciating strategy");
			System.exit(1);
		}
		return null;
    }
}
