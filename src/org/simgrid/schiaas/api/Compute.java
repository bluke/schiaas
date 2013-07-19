/*
 * Controller
 *
 * Copyright 2006-2013 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package org.simgrid.schiaas.api;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import javax.xml.parsers.*;
import org.simgrid.msg.*;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.engine.simiaas.ComputeControllerProcess;
import org.simgrid.schiaas.engine.simiaas.ControlTask;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
 * Main class of the SimIaaS.Compute package. 
 * Represents the controller of compute resources.
 * @author julien.gossa@unistra.fr
 * TODO Addinstance type, merge with cloud
 * 
 */
public class Compute {



	/** Contains all the clouds of the simulation. */
	protected static HashMap<String,CloudTOBEMERGEWITHCOMPUTE> clouds;

	/** Contains all the instances of the simulation, alive and terminated. */
	protected static Vector<Instance> instances;

	/** Used by the VM scheduler : A simple round-robin, nextHost being to next host to select. */
	protected static int nextHost;
	
	/**
	 * Initialize the clouds from a XML description file
	 */
	protected static void initClouds(String filename)
	{
		clouds = new HashMap<String,CloudTOBEMERGEWITHCOMPUTE>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc;
		
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(new File(filename));

			NodeList nodes = doc.getLastChild().getChildNodes();
			for (int i=0; i<nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().compareTo("cloud") == 0) {
					String cloudid = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
					clouds.put(cloudid,	new CloudTOBEMERGEWITHCOMPUTE(nodes.item(i)));
				}
			}			
			
		} catch (IOException e) {
			Msg.critical("CloudTOBEMERGEWITHCOMPUTE config file not found");
			e.printStackTrace();
			System.exit(0);
		}  catch (Exception e) {
			System.exit(0);
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * Initialize the scheduler.
	 */
	protected static void initScheduler()
	{
		nextHost = 0;
		instances = new Vector<Instance>();
	}

	/**
	 * Initialize the SimIaaS Controller.
	 * Must be called before Msg.run().
	 */
	public static void init(String filename)
	{
		initClouds(filename);
		initScheduler();
		
		/*
		for (Instance.STATE s : Instance.STATE.values())
		{
			Trace.vmVariableDeclare(s.toString());
		}
		*/
		
		/*
		Trace.hostStateDeclare ("VM_STATE"); 
		
		int color=16;
		
		for (Instance.STATE state : Instance.STATE.values()) {
			//Msg.info("state : "+ state.name() + " " + Integer.toBinaryString(color).charAt(2)+" "+Integer.toBinaryString(color).charAt(3)+" "+Integer.toBinaryString(color).charAt(4));
			Trace.hostStateDeclareValue ("VM_STATE", state.name(), Integer.toBinaryString(color).charAt(2)+" "+Integer.toBinaryString(color).charAt(3)+" "+Integer.toBinaryString(color).charAt(4));
			color++;
		}
		*/
	}
	
	/**
	 * Run a new instance, using a simple round-robin scheduling
	 * @param image The image of the instance.
	 * @param cloudid The id of the cloud/region to run the instance.
	 * @return The instance, about to be started
	 * @throws HostNotFoundException when the physical host is not found. 
	 */
	public static Instance runInstance(Image image, String cloudid)
	{
		CloudTOBEMERGEWITHCOMPUTE c = clouds.get(cloudid);
		
		if ( c.describeAvailability() == 0 ) return null;

		while (c.hosts[nextHost].instancesCount==c.hosts[nextHost].maxInstancesCount) nextHost=(nextHost+1)%c.hosts.length; 
		CloudTOBEMERGEWITHCOMPUTE.VMHost h = c.hosts[nextHost];
		
		
		String instanceName =  "id_"+c.name+"_"+h.host.getName()+"_"+c.hosts[nextHost].instanceID++;
		
		Mutex imageTransferMutex = null;
		switch(c.caching) {
		case OFF :
			imageTransferMutex = image.createTransferProcess(h);
			c.computeControllerProcess.enqueueControlTask(new ControlTask(image.size,image.getTransferMessageBox()));
			break;
		case ON :
			imageTransferMutex=h.imageCache.get(image);
			if (imageTransferMutex == null) {
				imageTransferMutex = image.createTransferProcess(h);
				h.imageCache.put(image, imageTransferMutex);
				c.computeControllerProcess.enqueueControlTask(new ControlTask(image.size,image.getTransferMessageBox()));		
			}
			break;
		case PRE :
			imageTransferMutex = null;
			break;
		}
		

		Instance theInstance = new Instance(image, c, h, instanceName, 1, imageTransferMutex);
		instances.add(theInstance);
		
		theInstance.doCommand(ComputeControllerProcess.COMMAND.BOOT);

		
		nextHost=(nextHost+1)%c.hosts.length;

		
		return theInstance;
	}
	
	/**
	 * Run several instances, using a simple round-robin scheduling
	 * @param image The image of the instance.
	 * @param cloudid The id of the cloud/region to run the instance.
	 * @param nVM The amount of instances to start.
	 * @return The instance, about to be started.
	 * @throws HostNotFoundException when the physical host is not found. 
	 */
	public static Instance[] runInstances(Image image, String cloudid, int nVM) throws HostNotFoundException {	
		Instance[] theInstances = new Instance[nVM];
		for (int i=0; i<nVM; i++){
			theInstances[i] = runInstance(image, cloudid);
		}
		return theInstances;
	}
	
	
	/**
	 * Terminate a given instance.
	 * @param theInstance The instance to be be terminated.
	 */
	public static void terminateInstance(Instance instance)  {
		engine.doCommand( )
	}
	
	/**
	 * Terminate a given set of instance.
	 * @param theInstances The set of instances to be terminated.
	 */
	public static void terminateInstances(Instance[] theInstances) {
		for (Instance v : theInstances) {
			terminateInstance(v);
		}
	}
	
	/**
	 * Give the current set of instances, alive and terminated.
	 * @return the current set of instances, alive and terminated.
	 */
	public static Instance[] describeInstances() {
		Instance[] anArray = new Instance[instances.size()];
		instances.toArray(anArray);
		return (anArray) ;
	}
	
	/**
	 * Give the current availability of new instances on a given cloud.
	 * @cloudName The name of the target cloud.
	 * @return The number of available standard instances.
	 */
	public static int describeAvailability(String cloudName) {
		return clouds.get(cloudName).describeAvailability();
	}
	
	/**
	 * Give the standard power of instances on a given cloud.
	 * @cloudName The name of the target cloud.
	 * @return The standard power.
	 */
	public static double getStandardPower(String cloudName) {
		return clouds.get(cloudName).standardPower;
	}

	
	/**
	 * Give the maximum number of concurrent vm bootings
	 * @cloudName The name of the target cloud.
	 * @return The maximum number of concurrent vm bootings.
	 */
	public static int getMaxConcurrentBoots(String cloudName) {
		return clouds.get(cloudName).maxConcurrentBoots*clouds.get(cloudName).hosts.length;
	}

	
	/**
	 * Description of all of the clouds.
	 * @return A string describing the clouds.
	 */
	public static String getCloudsDescription()
	{
		String res = new String();
		Collection<CloudTOBEMERGEWITHCOMPUTE> cc = clouds.values();
		for (CloudTOBEMERGEWITHCOMPUTE c : cc) {
			res+=c.name+"\n";
			for (CloudTOBEMERGEWITHCOMPUTE.VMHost h : c.hosts) {
				res+=h.host.getName()+"\n";
			}
		}
		return res;
	}
	
	/**
	 * Description of all of the instances.
	 * @return A string describing the instances.
	 */
	public static String getInstancesDescription() {
		String res = new String();
		for (Instance i : instances) {
			res+=i.toString()+"\n";
		}
		return res;
	}

	
	/**
	 * Shutdown all the instances and remaining cloud managing processes
	 * @throws HostFailureException 
	 */
	public static void terminate() throws HostFailureException {
		Msg.verb("Terminating all remaining instances");
		for (Instance instance : instances) {
			if (instance.state != Instance.STATE.SHUTTINGDOWN && instance.state != Instance.STATE.TERMINATED) {
				instance.terminate();
			}
		}
		for (Instance instance : instances) {
			while (instance.state != Instance.STATE.TERMINATED) {
				Process.currentProcess().waitFor(10);
			}
		}
		
		Msg.verb("Killing CloudTOBEMERGEWITHCOMPUTE ControlProcesses");
		for (CloudTOBEMERGEWITHCOMPUTE cloud : clouds.values()) {
			cloud.computeControllerProcess.enqueueControlTask(new ControlTask(null,ComputeControllerProcess.COMMAND.FINALIZE));
		}
		Process.currentProcess().waitFor(10);
	}

}
