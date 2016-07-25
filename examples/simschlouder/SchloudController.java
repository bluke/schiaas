/*
 * Master of a basic master/slave example in Java
 *
 * Copyright 2006-2012 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package simschlouder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Mutex;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import simschlouder.algorithms.AStrategy;
import simschlouder.util.SimSchlouderException;

/**
 * Represents the controller on the SimSchlouder system
 * @author julien.gossa@unistra.fr
 *
 */
public class SchloudController extends org.simgrid.msg.Process {
	
	/** Set of alive worker nodes */
	public static Vector<SchloudNode> nodes;
	
	/** Set of terminated worker nodes */
	public static Vector<SchloudNode> terminatedNodes;
	
	/** Main queue of pending tasks */
	public static LinkedList<SchloudTask> mainQueue;
	
	/** Amount of idle worker nodes */
	public static int idleNodesCount;
	
	/** Id of the host of the SimSchlouder controller */
	/** TODO: check whether necessary */
	public static String broker;
	
	/** Host of the SimSchloder controller */
	public static Host host;
	
	/** Provisioning strategy */
	public static AStrategy strategy;
	
	/** Cloud to run instances */
	public static SchloudCloud schloudCloud;
	
	/** Set of clouds to run instancess, for multi-clouds strategies */
	public static HashMap<String, SchloudCloud> schloudClouds;
	
	/** Period to scan the mainqueue, provision instances, and distribute tasks */ 
	protected static double period = 5;
	/** Mutex to track whether the mainqueue is empty */
	protected static Mutex emptyQueueMutex;

	/** True when all of the tasks have been submitted */
	protected static boolean allTasksSubmitted=false;

	/** Id of the instance image */  
	public static String imageId;
	/** Id of the instance type */
	public static String instanceTypeId;

	
	/**
	 * Creates a new Schlouder controller on a given host.
	 * The controller is started as a process by SimGrid.
	 * It is responsible for scheduling incoming tasks
	 * and for starting/stopping VMs
	 * The VMs are represented by <i>SchloudNode</i> processes
	 * @param host the host
	 * @param name the name of the controller
	 * @param args useless
	 */
	public SchloudController(Host host, String name, String [] args) {
		super(host,"SchloudController",args);
		
		nodes = new Vector<SchloudNode>();
		terminatedNodes = new Vector<SchloudNode>();
		mainQueue = new LinkedList<SchloudTask>();
		
		idleNodesCount = 0;
		
		emptyQueueMutex = new Mutex();
		
		SchloudController.host = host;
	}

	/**
	 * Entry point for the controller process
	 */
	public void main(String[] args) throws MsgException {
		
		period = 1;
		
		while (!(mainQueue.isEmpty() && nodes.isEmpty() && allTasksSubmitted)) {
			Msg.verb("main loop : main queue="+ mainQueue.size() 
				+ ", idle nodes=" +idleNodesCount+"/("+ nodes.size()+"+"+schloudCloud.describeAvailability(instanceTypeId)+")");
						
			terminateIdleNodes();
			
			// if (idleNodesCount!=0 || schloudCloud.describeAvailability(instanceTypeId)>0) {
			if (!mainQueue.isEmpty()) {
				try {
					SchloudController.strategy.execute();
				} catch (SimSchlouderException e) {				
					e.printStackTrace();
					//TODO is it a good idea to terminate this process?
					System.exit(1);
				}
			}
			
			if ( allTasksSubmitted ) 
			//	|| ((!mainQueue.isEmpty() 
			//	&& mainQueue.peekFirst().dependencies!= null && mainQueue.peekFirst().dependencies.size() > 0 )))
				period = 5;
			
			waitFor(period);
		}
		
        SchIaaS.terminate();
    }

	
	/**
	 * Stops a running VM node
	 * @param node the node to be stopped
	 */
	public static void stopNode(SchloudNode node) {
		//Msg.info("Stopping node "+node.instance.getName());
		nodes.remove(node);
		terminatedNodes.add(node);
		schloudCloud.compute.terminateInstance(node.instance);
		node.setState(SchloudNode.STATE.TERMINATED);
	}
	
	/**
	 * Starts a new VM node
	 * @return the newly created object reference
	 * @throws VMSchedulingException 
	 */
	public static SchloudNode startNewNode() throws VMSchedulingException {
		SchloudNode node = SchloudNode.startNewNode(SchloudController.schloudCloud);

		SchloudController.nodes.add(node);
		
		SchloudController.schloudCloud.incrementBootCount();
		return node;
	}
	
	/**
	 * Adds a new task to the list of tasks yet to be assigned to VM nodes
	 * @param task the new task
	 */
	protected static void enqueueTask(SchloudTask task) {
		mainQueue.add(task);
	}
	
	/**
	 * Assigns a task to a VM node.
	 * @param st the task
	 * @param node the node to process the task
	 */
	public static void setTaskToNode(SchloudTask st, SchloudNode node) {
		node.enqueue(st);
		if (node.queue.size()==1) node.processQueue();
	}

	/**
	 * converts the time in seconds to BTUs
	 * @param time the time in seconds
	 * @return the number of BTUs corresponding to our time
	 */
	public static int time2BTU(double time) {
		// time-1 in order to consider a full BTU (i.e. 3600s) as one BTU
		if (time>1) time--;
		return (1+((int)((time)/schloudCloud.getBtuTime())));
	}

	/**
	 * returns a description of the terminated nodes
	 * @return a string representing the terminated nodes
	 */
	public static String getPostMortem() {
		String s = "";
		for (SchloudNode node : terminatedNodes) {
			s += "\n"+node.toString();
		}
		return s;
	}
	
	/**
	 * returns the result of the simulation
	 * @return a string containing simulation statistics
	 * @throws SimSchlouderException 
	 */
	public static String getOutcome() throws SimSchlouderException {
		double makespan=0;
		double totalBTU=0;
		double totalRuntime=0;
		
		for (SchloudNode node : terminatedNodes) {
			for (SchloudTask task : node.completedQueue) {
				totalRuntime += task.getRuntime();
				if ( task.getDateOfLast(SchloudTask.STATE.COMPLETE) > makespan) {
					makespan = task.getDateOfLast(SchloudTask.STATE.COMPLETE);
				}
			}
			totalBTU += time2BTU(node.getUptime());
		}
		
		return "makespan:\t"+makespan+"\ntotalBTU:\t"+totalBTU+"\nusage:  \t"+(totalRuntime/(totalBTU*3600.0));
	}

	/**
	 * Builds up the cloud from the cloud.xml file 
	 * @param filename
	 */
	public static void init(String filename) {
		Msg.verb("Init SchloudController");
		
		String storage = null;
		
		schloudClouds = new HashMap<String, SchloudCloud>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc;
		
		String provisioningCloud=null;
		
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(new File(filename));

			NodeList nodes = doc.getLastChild().getChildNodes();
			for (int i=0; i<nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().compareTo("config") == 0) {
					String platform = nodes.item(i).getAttributes().getNamedItem("platform").getNodeValue();
					String deployment = null;
					if(nodes.item(i).getAttributes().getNamedItem("deployment")!=null){
						deployment = nodes.item(i).getAttributes().getNamedItem("deployment").getNodeValue();
					}
					
					String cloud = nodes.item(i).getAttributes().getNamedItem("cloud").getNodeValue();
					
					/* construct the platform and deploy the application */
					Msg.createEnvironment(platform);
					if(deployment!=null)
						Msg.deployApplication(deployment);
					
					SchIaaS.init(cloud);
				}else if (nodes.item(i).getNodeName().compareTo("broker") == 0) {
					broker = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
					String[] arg ={};
					try {
						SchloudController process = new SchloudController(Host.getByName(broker),broker,arg);
						process.start();
					} catch (HostNotFoundException e) {
						Msg.critical("Pinning fail : broker \""+ broker +"\" not found");
					}
				}
				else if (nodes.item(i).getNodeName().compareTo("cloud") == 0) {
					String name = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
					double B0 = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("B0").getNodeValue());
					double B1 = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("B1").getNodeValue());
					double BTU = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("BTU").getNodeValue());
					double shutdownMargin = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("shutdown_margin").getNodeValue());
					double standardPower = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("standard_power").getNodeValue());
					int maxInstances = 0;
					if(nodes.item(i).getAttributes().getNamedItem("max_instances_per_user")!=null){
						maxInstances = Integer.parseInt(nodes.item(i).getAttributes().getNamedItem("max_instances_per_user").getNodeValue());
					}
					schloudClouds.put(name, new simschlouder.SchloudCloud(name,B0,B1,BTU, shutdownMargin, standardPower ,maxInstances));
				}
				else if (nodes.item(i).getNodeName().compareTo("provisioning") == 0) {
					NodeList provNodes = nodes.item(i).getChildNodes(); 
					for(int j=0; j<provNodes.getLength(); j++) {  
						if (provNodes.item(j).getNodeName().compareTo("config")==0) {
							provisioningCloud = provNodes.item(j).getAttributes().getNamedItem("cloud").getNodeValue();
							storage = provNodes.item(j).getAttributes().getNamedItem("storage").getNodeValue();
							imageId = provNodes.item(j).getAttributes().getNamedItem("image").getNodeValue();
							instanceTypeId = provNodes.item(j).getAttributes().getNamedItem("instance_type").getNodeValue();
							if (provNodes.item(j).getAttributes().getNamedItem("validation") != null)
								SimSchlouder.validation = provNodes.item(j).getAttributes().getNamedItem("validation").getNodeValue().equals("true");
						}
					}
				}
			}
			
			schloudCloud=schloudClouds.get(provisioningCloud);

			//Storage management
			if (storage.compareTo("host") == 0) {
				SimSchlouder.storageType = SimSchlouder.StorageType.CLIENT;
			} else if (storage.compareTo("instance") == 0) {
				SimSchlouder.storageType = SimSchlouder.StorageType.INSTANCE;
			} else {
				SimSchlouder.storageType = SimSchlouder.StorageType.CLOUD;
			}			
			schloudCloud.setStorage(storage);
			
		} catch (IOException e) {
			Msg.critical("SimSchlouder config file not found");
			e.printStackTrace();
			System.exit(0);
		}  catch (Exception e) {
			System.exit(0);
			e.printStackTrace();
		} 
		
		Msg.verb("Init SchloudController ended");
	}

	/**
	 * Writes a JSON file for use in later processing.
	 * Unfortunately handmade, because no standard lib was found at the time.
	 * @param args The arguments of the simulation
	 * @throws IOException
	 * @throws SimSchlouderException 
	 */
	public static void writeJSON(String args[]) throws IOException, SimSchlouderException {
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
		String description="SimSchlouder simulation trace";
		
		FileWriter fstream = new FileWriter(SimSchlouder.outJsonFile, false);
	    BufferedWriter out = new BufferedWriter(fstream);
		
		out.write("{\n");
		out.write("\t\"info\": {\n");
		out.write("\t\t\"version\": \"SimSchlouder\",\n");
		out.write("\t\t\"simschlouder_file\": \""+args[0]+"\",\n");
		out.write("\t\t\"tasks_file\": \""+args[2]+"\",\n");
		out.write("\t\t\"real_runtimes\": \""+real_runtimes+"\",\n");
		out.write("\t\t\"communications\": \""+communications+"\",\n");
		out.write("\t\t\"real_boottimes\": \""+real_boottimes+"\",\n");
		out.write("\t\t\"real_threads\": \""+real_threads+"\",\n");
		out.write("\t\t\"storage\": {\n");
		out.write("\t\t\t\"type\": \"n/a\",\n");
		out.write("\t\t\t\"location\": \"n/a\"\n");
		out.write("\t\t},\n");
		out.write("\t\t\"start_date\": 0,\n");
		out.write("\t\t\"description\": \""+description+"\"\n");
		out.write("\t},\n");
		out.write("\t\"nodes\": [\n");
		for (int i=0; i<SchloudController.terminatedNodes.size(); i++) {
			out.write("\t\t{\n");
			SchloudController.terminatedNodes.get(i).writeJSON(out);
			out.write("\t\t}");
			if (i<SchloudController.terminatedNodes.size()-1) out.write(",");
			out.write("\n");
		}
		out.write("\t]\n");
		out.write("}\n");
		
		out.close();
	}
	
	/**
	 * Terminates any idle VMs
	 * TODO how to handle this during execution?
	 */
	protected void terminateIdleNodes() {
		if (idleNodesCount==0) return;
		int i=0;
		while (i<nodes.size()) {
			//if ( time2BTU(nodes.get(i).getUptime())	< time2BTU(nodes.get(i).getUptime()+schloudCloud.shutdownMargin))
				//Msg.info("TERMINATE " + nodes.get(i).instanceId + " : " + nodes.get(i).getFullIdleTime());
			
			if ( time2BTU(nodes.get(i).getUptime())	< time2BTU(nodes.get(i).getUptime()+schloudCloud.shutdownMargin) 
					&& nodes.get(i).isIdle() 
					&& nodes.get(i).getFullIdleTime() > schloudCloud.shutdownMargin ) {
				//Msg.info("TERMINATE " + nodes.get(i).instanceId + " : " + nodes.get(i).getFullIdleTime());
				SchloudController.stopNode(nodes.get(i));
			} else {
				i++;
			}
		}
	}
}
