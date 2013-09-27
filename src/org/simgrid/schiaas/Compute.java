/*
 * Controller
 *
 * Copyright 2006-2013 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package org.simgrid.schiaas;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.simgrid.msg.*;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.ComputeEngine;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Represents the compute component.
 * @author julien.gossa@unistra.fr
 */
public class Compute {

	/** id of this */
	protected String id;
	
	/** cloud using this */
	protected Cloud cloud;
	
	/** engine of this */
	protected ComputeEngine computeEngine;
	
	/** Contains all the instances of this compute instance, alive and terminated. */
	protected Map<String, Instance> instances;
	
	/** Contains all the instanceTypes of this compute instance, alive and terminated. */
	protected Map<String, InstanceType> instanceTypes;
	
	/** Contains all the image of this compute instance, alive and terminated. */
	protected Map<String, Image> images;
	
	/** Contains all the properties of this compute instance, alive and terminated. */
	protected Map<String,String> properties;
	
	/** Contains all the hosts of this compute instance, alive and terminated. */
	protected Collection<Host> hosts;
	
	
	/**
	 * Unique constructore from XML config file
	 * @param cloud the cloud of this
	 * @param computeXMLNode the node pointing out a compute node in the XML config file
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	protected Compute(Cloud cloud, Node computeXMLNode) throws InstantiationException, IllegalAccessException, ClassNotFoundException	{
		
		instances = new HashMap<String, Instance>();
		properties = new HashMap<String,String>();
		
		instanceTypes = new HashMap<String, InstanceType>();
		images = new HashMap<String, Image>();
		
		this.cloud = cloud;
		
		String engine = computeXMLNode.getAttributes().getNamedItem("engine").getNodeValue();
		
		Msg.debug("Compute initialization, engine="+engine);
		
		Collection<Host> hosts = new Vector<Host>();
		NodeList nodes = computeXMLNode.getChildNodes();
		for (int i=0; i<nodes.getLength(); i++) {
			
			if (nodes.item(i).getNodeName().compareTo("config") == 0) {
				NamedNodeMap configNNM = nodes.item(i).getAttributes();
				for (int j=0; j<configNNM.getLength(); j++) {
					properties.put(configNNM.item(j).getNodeName(), configNNM.item(j).getNodeValue());
				}
			}
			
			if (nodes.item(i).getNodeName().compareTo("host") == 0) {
				try {
					hosts.add(Host.getByName(nodes.item(i).getAttributes().getNamedItem("id").getNodeValue()));
				} catch (HostNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (nodes.item(i).getNodeName().compareTo("instance_type") == 0) {
				InstanceType instancetype = new InstanceType(nodes.item(i));
				instanceTypes.put(instancetype.getId(), instancetype);
			}
			
			if (nodes.item(i).getNodeName().compareTo("image") == 0) {
				Image image = new Image(nodes.item(i));
				images.put(image.getId(), image);
			}
			
		}
		
		try {
			computeEngine = (ComputeEngine)Class.forName(engine).getConstructor(Compute.class, Collection.class).newInstance(this, hosts);
		} catch (IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			Msg.critical("Something wrong happened while loading the cloud engine "+engine);
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @return the id of this
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * 
	 * @return the cloud of this
	 */
	public Cloud getCloud() {
		return cloud;
	}
	
	/**
	 * for all properties
	 * @param propId the id of the property, as is the XML config file
	 * @return the property
	 */
	public String getProperty(String propId) {
		return properties.get(propId);
	}
	
	/**
	 * 
	 * @return all the instances, alive and terminated, of this
	 */
	public Collection<Instance> describeInstances() {
		return instances.values();
	}
	
	/**
	 * 
	 * @param id the id of one instance
	 * @return the instance of the id
	 */
	public Instance describeInstance(String id) {
		return instances.get(id);
	}
	
	/**
	 * 
	 * @return all the types of instance available on this
	 */
	public Collection<InstanceType> describeInstanceTypes() {
		return instanceTypes.values();
	}

	/**
	 * 
	 * @param id the id of one type of instance
	 * @return the type of instance of the id
	 */
	public InstanceType describeInstanceType(String id) {
		return instanceTypes.get(id);
	}

	/**
	 * 
	 * @return all the images available on this
	 */
	public Map<String, Image> describeImages() {
		return images;
	}

	/**
	 * 
	 * @param id the id of one image
	 * @return the image of the id
	 */
	public Image describeImage(String id) {
		return images.get(id);
	}
	

	/**
	 * Run a new instance
	 * @param imageId the id of the image of the instance.
	 * @param instanceTypeId the id of the type of the instance 
	 * @return the id of the instance, about to be started
	 */
	public String runInstance(String imageId, String instanceTypeId) {
		Instance instance = computeEngine.runInstance(images.get(imageId), instanceTypes.get(instanceTypeId));
		instances.put(instance.id, instance);
		return instance.id;
	}

	
	/**
	 * Run a new instance
	 * @param imageId the id of the image of the instance.
	 * @param instanceTypeId the id of the type of the instance 
	 * @param nVM the number of instances to run
	 * @return the id of the instance, about to be started
	 */
	public String[] runInstances(String imageId, String instanceTypeId, int nVM) {
		String[] instancesId = new String[nVM];
		for (int i=0; i<nVM; i++) {
			instancesId[i]=runInstance(imageId, instanceTypeId);
		}
		return instancesId;
	}

	/**
	 * Terminate a given instance.
	 * @param instanceId The id of the instance to be be terminated.
	 */
	public void terminateInstance(String instanceId)  {
		computeEngine.doCommand(ComputeEngine.COMMAND.SHUTDOWN, instances.get(instanceId));
	}


	/**	
	 * Suspend a given instance.
	 * @param theInstance The id of the instance to be be suspended.
	 * @TODO check why it isn't working
	 */
	public void suspendInstance(String instanceId)  {
		computeEngine.doCommand(ComputeEngine.COMMAND.SUSPEND, instances.get(instanceId));
	}
		
	/**
	 * Resume a given instance.
	 * @param theInstance The if of the instance to be be resumed.
	 */
	public void resumeInstance(String instanceId)  {
		computeEngine.doCommand(ComputeEngine.COMMAND.RESUME, instances.get(instanceId));
	}

	
	
	/**
	 * Give the current availability of new instances on this.
	 * @param instanceTypeId The id of a type of instance
	 * @return The number of available instances of the given type
	 */
	public int describeAvailability(String instanceTypeId) {
		return computeEngine.describeAvailability(instanceTypes.get(instanceTypeId));
	}
	
	
	/**
	 * Terminate this, by terminating all instances and then the engine
	 * @throws HostFailureException 
	 */
	public void terminate() throws HostFailureException {
		Msg.verb("Terminating all remaining instances");
		for (Instance instance : instances.values()) {
			Msg.verb(instance + " " + instance.isAvail());
			if (instance.isAvail()) {
				terminateInstance(instance.id);
			}
		}
		computeEngine.terminate();
	}
	
}
