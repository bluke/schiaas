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
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Mutex;
import org.simgrid.msg.Process;
import org.simgrid.simiaas.api.Compute;
import org.simgrid.simiaas.api.Image;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import simschlouder.algorithms.AStrategy;
import simschlouder.util.SimSchloudException;

public class SchloudController extends Process {
	
	public static Vector<SchloudNode> nodes;
	public static Vector<SchloudNode> terminatedNodes;
	public static LinkedList<SchloudTask> mainQueue;
	
	public static int idleNodesCount;
	
	protected static Image schloudNodeImage;
	
	public static Host host;
	
	public static AStrategy strategy;
	public static Cloud cloud;
	public static HashMap<String, Cloud> clouds;
	
	protected static double period = 10;
	protected static Mutex emptyQueueMutex;

	protected static boolean allTasksSubmitted=false;

	/**
	 * Entry point for the controller process
	 */
	public void main(String[] args) throws MsgException {
		while (!(mainQueue.isEmpty() && nodes.isEmpty() && allTasksSubmitted)) {
			//Msg.verb("main loop : "+ mainQueue.size() + " tasks remaining to process, and " + nodes.size() +"nodes still alive");
			try {
				SchloudController.strategy.execute();
			} catch (SimSchloudException e) {				
				e.printStackTrace();
				//TODO is it a good idea to terminate this process?
				System.exit(1);
			}
			terminateIdleNodes();
			
			waitFor(period);
		}
		
        Compute.terminate();
	}
	
	/**
	 * Creates a new Schlouder controller on a given host.
	 * The controller is started as a process by SimGrid.
	 * It is responsible for scheduling incoming tasks
	 * and for starting/stopping VMs
	 * The VMs are represented by <i>SchloudNode</i> processes
	 * @param host the host
	 * @param name the name of the controller
	 * @param args
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
	 * Stops a running VM node
	 * @param node the node to be stopped
	 */
	public static void stopNode(SchloudNode node) {
		//Msg.info("Stopping node "+node.instance.getName());
		nodes.remove(node);
		terminatedNodes.add(node);
		node.instance.terminate();
		node.setState(SchloudNode.STATE.TERMINATED);
	}
	
	/**
	 * Starts a new VM node
	 * @return the newly created object reference
	 */
	public static SchloudNode startNewNode() {
		SchloudNode node = SchloudNode.startNewNode(SchloudController.cloud);
		
		if (node == null) return null;
		
		SchloudController.nodes.add(node);
		
		SchloudController.cloud.incrementBootCount();
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
		return (1+((int)(time/cloud.BTU)));
	}
		
	public static String getPostMortem() {
		String s = "";
		for (SchloudNode node : terminatedNodes) {
			s += "\n"+node.toString();
		}
		return s;
	}
	
	/**
	 * Prints the result of the simulation
	 * @return a string containing simulation statistics
	 */
	public static String getOutcome() {
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
		
		clouds = new HashMap<String, Cloud>();
		
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
					String deployment = nodes.item(i).getAttributes().getNamedItem("deployment").getNodeValue();
					String cloud = nodes.item(i).getAttributes().getNamedItem("cloud").getNodeValue();
					
					/* construct the platform and deploy the application */
					Msg.createEnvironment(platform);
					Msg.deployApplication(deployment);
					
					Compute.init(cloud);
				}
				else if (nodes.item(i).getNodeName().compareTo("cloud") == 0) {
					String name = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
					double B0 = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("B0").getNodeValue());
					double B1 = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("B1").getNodeValue());
					double BTU = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("BTU").getNodeValue());
					double shutdownMargin = Double.parseDouble(nodes.item(i).getAttributes().getNamedItem("shutdownMargin").getNodeValue());
					
					clouds.put(name, new simschlouder.Cloud(name,B0,B1,BTU, shutdownMargin));
				}
				else if (nodes.item(i).getNodeName().compareTo("provisioning") == 0) {
					NodeList provNodes=nodes.item(i).getChildNodes(); 
					for(int j=0; j<provNodes.getLength(); j++) {  
						if (provNodes.item(j).getNodeName().compareTo("config")==0) {
							provisioningCloud=provNodes.item(j).getAttributes().getNamedItem("cloud").getNodeValue();
						}
						if (provNodes.item(j).getNodeName().compareTo("image")==0) {
							String name=provNodes.item(j).getAttributes().getNamedItem("name").getNodeValue();
							double size=Double.parseDouble(provNodes.item(j).getAttributes().getNamedItem("size").getNodeValue());
							double boottime=Double.parseDouble(provNodes.item(j).getAttributes().getNamedItem("boottime").getNodeValue());
							double shutdowntime=Double.parseDouble(provNodes.item(j).getAttributes().getNamedItem("shutdowntime").getNodeValue());
							schloudNodeImage = new Image(name, size,SimSchlouder.time2flops(boottime),SimSchlouder.time2flops(shutdowntime));
						}
					}
				}
			}
			
			cloud=clouds.get(provisioningCloud);
			
		} catch (IOException e) {
			Msg.critical("SimSchlouder config file not found");
			e.printStackTrace();
			System.exit(0);
		}  catch (Exception e) {
			System.exit(0);
			e.printStackTrace();
		} 
	}

	/**
	 * Writes a JSON file for use in later processing
	 * @param description the ID of the version
	 * @throws IOException
	 */
	public static void writeJSON(String description) throws IOException {
	    FileWriter fstream = new FileWriter("simschlouder.json", false);
	    BufferedWriter out = new BufferedWriter(fstream);
		
		out.write("{\n");
		out.write("\t\"info\": {\n");
		//out.write("\t\t\"version\": \"SimSchlouder\",\n");
		out.write("\t\t\"start_date\": 0,\n");
		out.write("\t\t\"version\": \""+description+"\"\n");
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
			/*if (nodes.get(i).instance.getName().compareTo("id_icps-opst_icps-gc-1_4") == 0)
			Msg.info("TERMINATE : " + nodes.get(i).instance.getName() + " : " + nodes.get(i).isIdle() + " - " 
					+ nodes.get(i).getUptime() + "("+ time2BTU(nodes.get(i).getUptime()) +") ~ "
					+ nodes.get(i).getUptime()+cloud.shutdownMargin+ "("+ time2BTU(nodes.get(i).getUptime()+cloud.shutdownMargin) +")" );*/
			
			if (nodes.get(i).isIdle() && time2BTU(nodes.get(i).getUptime())<time2BTU(nodes.get(i).getUptime()+cloud.shutdownMargin) ) {
				//Msg.info("hou yes " + nodes.get(i).instance.getName());
				SchloudController.stopNode(nodes.get(i));
			} else {
				i++;
			}
		}
	}
}
