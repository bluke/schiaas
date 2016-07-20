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
import java.util.Locale;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.NativeException;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.tools.Trace;
import org.xml.sax.SAXException;

import simschlouder.algorithms.AStrategy;
import simschlouder.util.SimSchlouderException;

/**
 * Main entry point of SimSchlouder.
 * Provides global information about the simulation and handles the task file.
 * @author julien.gossa@unistra.fr
 */
public class SimSchlouder {


	/**
	 * Convert real time to Msg task durations according to the cloud standard power. 
	 * @param time a time in seconds.
	 * @return task durations according to the cloud standard power.
	 */
	public static double timeToDuration(double time) {
		return time*SchloudController.schloudCloud.standardPower;
	}
	
	/**
	 * The different types of cloud storages.
	 * @author julien.gossa@unistra.fr
	 */
	public static enum StorageType {CLOUD, CLIENT, INSTANCE};

	/** The type of storage - use at your own risk */
	public static StorageType storageType;

	/** The json output file name. */
	public static String outJsonFile = "simschlouder.json";
	
	/** Flag to indicate if the simulator is used to validation purpose */
	public static Boolean validation = false;

	/** The process to read the taskfile */
	public static TaskFileReaderProcess taskFileReaderProcess;



	/**
	 * Read the task file.
	 * The format of this file is:
	 * [boots] (optional section)
	 * vm_boottime [vm_provisioning_date [vm_future_date]]
	 * [tasks] 
	 * task_name submission_date walltime_prediction [~ real_runtime/walltime] [input_data_size output_data_size in MB] [-> dependencies]
	 * @author julien.gossa@unistra.fr
	 */
	public static class TaskFileReaderProcess extends  org.simgrid.msg.Process {
		
		public TaskFileReaderProcess(Host host, String name, String[] args) {
			super(host,name,args);
		}
		
		
		// TODO: Errors
		public void readTaskFile(String fileName,
				boolean real_runtimes,
				boolean communications,
				boolean real_boottimes,
				boolean real_threads
				) throws FileNotFoundException, HostFailureException {
			
		    Locale.setDefault(new Locale("en", "US"));
			
			Scanner scf = new Scanner(new File(fileName));
			HashMap<String, SchloudTask> taskMap = new HashMap<String, SchloudTask>();
			
			double oldSubmissionDate = 0;
			int i;
			double d1, d2;
			
			Scanner sc = new Scanner(scf.nextLine());			
			if (sc.hasNext("\\[boots\\]")) {
				sc.close();
				sc = new Scanner(scf.nextLine());
				do {
					i = sc.nextInt();
					if(real_boottimes) SchloudController.schloudCloud.bootTimes.add(i);
					if (sc.hasNextInt()) {
						i = sc.nextInt();
						if (real_threads) SchloudController.schloudCloud.provisioningDates.add(i); 
					}
					if (sc.hasNextInt()) {
						i = sc.nextInt();
						if (real_threads) SchloudController.schloudCloud.lagTimes.add(i);
					}
					sc = new Scanner(scf.nextLine());
				} while (sc.hasNextInt());				
			} else {
				scf = new Scanner(new File(fileName));
			}
	
			while (scf.hasNext()) {
				sc = new Scanner(scf.nextLine());
				sc.useLocale(Locale.US);
				
				String jid = sc.next();
				double submissionDate = sc.nextDouble();
				
				if (submissionDate != oldSubmissionDate) {
					Msg.verb("Waiting for next bag of tasks");
					waitFor(submissionDate-oldSubmissionDate);
				}

				double walltimePrediction = sc.nextDouble();

				double runtime = walltimePrediction;
				double inputSize = 0;
				double outputSize = 0;

				if (sc.hasNext("~")) {
					sc.next();
					d1 = sc.nextDouble();
					if (real_runtimes) runtime = d1; 
				}

				if (sc.hasNextDouble()) {
					d1 = sc.nextDouble();
					d2 = sc.nextDouble();
					if (communications) {
						inputSize = d1;
						outputSize = d2;
					}
				}
				
				Vector<SchloudTask> dependencies = new Vector<SchloudTask>();
				if (sc.hasNext("\\-\\>")) {
					sc.next("\\-\\>");
					while (sc.hasNext()) {
						String depname = sc.next();
						SchloudTask dep = taskMap.get(depname);
						if(dep != null){
							dependencies.add(dep);
						}
						else{
							Msg.critical("Simschlouder error, job "+jid+" has a unexistant dependency : "+depname);
						}				
					}
				}

				//Msg.verb(Dependencies:" : "+dependencies.size());
				SchloudTask task = new SchloudTask(jid, walltimePrediction, runtime, inputSize, outputSize, dependencies); 
				taskMap.put(task.name, task);
				
				Msg.info("Enqueuing " + task);
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
				boolean real_runtimes = false;
				boolean communications = false;
				boolean real_boottimes = false;
				boolean real_threads = false;
				
				for (int i=1; i<args.length; i++) {
					if (args[i].equals("real_runtimes")) real_runtimes = true;
					if (args[i].equals("communications")) communications = true;
					if (args[i].equals("real_boottimes")) real_boottimes = true;
					if (args[i].equals("real_threads")) real_threads = true;
				}
				
				readTaskFile(args[0], real_runtimes, communications, real_boottimes, real_threads);
			} catch (FileNotFoundException e) {
				Msg.critical("Task file "+args[0]+" not found.");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Main entry point of the simulation.
	 * @param args is given by the jvm
	 * @throws NativeException
	 * @throws HostNotFoundException
	 * @throws IOException
	 * @throws HostFailureException
	 * @throws SimSchlouderException
	 */
    public static void main(String[] args) throws NativeException, HostNotFoundException, IOException, HostFailureException, SimSchlouderException {       
	    Msg.init(args);
	
	    if (args.length < 3) {
			Msg.info("Usage   : SimSchlouder simschlouder_file tasks_file strategyClass [");
			Msg.info("example : SimSchlouder simschlouder.xml workload.tasks ASAP");
			System.exit(1);	
		}
		
	    if (args.length==4) outJsonFile=args[3];
	    
	    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema = null;
	    
		try {
			schema = schemaFactory.newSchema(new StreamSource(SchIaaS.class.getResourceAsStream("/simschlouder/simschlouder.xsd")));
		} catch (SAXException e1) {
			Msg.info("Schema factory failed :");
			Msg.info(e1.toString());
			System.exit(1);
		}
	    
		Validator validator = schema.newValidator();
		
		try{
            validator.validate(new StreamSource(new File(args[0])));
            Msg.info(args[0]+" is valid");
        }
        catch (SAXException e) 
        {
        	Msg.info(args[0]+" is NOT valid");
        	Msg.info("Reason: " + e.getLocalizedMessage());
        	Msg.debug(e.toString());
            System.exit(1);
        }
		
		Trace.init("SimSchlouder");
		SchloudController.init(args[0]);
				
		Msg.verb("Loading the strategy: "+args[1]);
		//SchloudController.strategy=SchloudController.STRATEGY.valueOf(args[2].toUpperCase());
		SchloudController.strategy = SimSchlouder.loadStrategy(args[2].trim());
		Msg.info("Strategy set to "+SchloudController.strategy.getName());
		
		Msg.verb("Reading the task file: "+args[2]);
		String[] tfrpargs = new String[args.length-2];
		for (int i=2; i<args.length; i++) tfrpargs[i-2] = args[i]; 
		taskFileReaderProcess = new TaskFileReaderProcess(Host.getByName(SchloudController.broker),"TaskFileReader",tfrpargs);
		taskFileReaderProcess.start();

		
		Msg.verb("Running the simulation...");
		/*  execute the simulation. */
        Msg.run();
        
        
        //Msg.info("Cloud description\n"+Compute.getCloudsDescription());
        //Msg.info("Nodes description\n"+SchloudController.getPostMortem());
        //Msg.info("Instances description\n"+Compute.getInstancesDescription());
        System.out.println("Outcomes:\n"+SchloudController.getOutcome());
        
        SchloudController.writeJSON("simschlouder");
        Trace.close();

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
