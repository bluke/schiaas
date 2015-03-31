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
import org.simgrid.schiaas.exceptions.MissingConfigException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Compute component: management of instances lifecycle
 * 
 * @author julien.gossa@unistra.fr
 */
public class Compute {

	/** cloud using this */
	protected Cloud cloud;

	/** engine of this */
	protected ComputeEngine computeEngine;

	/** Contains all the instances of this compute instance, alive and terminated. */
	protected Map<String, Instance> instances;
	
	/** A counter to set instances' id. */
	protected int instancesId;

	/** Contains all the instanceTypes of this compute instance. */
	protected Map<String, InstanceType> instanceTypes;

	/** Contains all the images of this compute instance. */
	protected Map<String, Image> images;

	/** Contains all the config properties of this compute instance. */
	protected Map<String, String> config;

	/** Standard power, e.g. EC2CU.*/
	protected double standardPower;
	
	/**
	 * Unique constructor from XML config file
	 * 
	 * @param cloud
	 *            the cloud of this
	 * @param computeXMLNode
	 *            the node pointing out a compute node in the XML config file
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	protected Compute(Cloud cloud, Node computeXMLNode)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		this.instances = new HashMap<String, Instance>();
		this.instancesId = 0;
		
		this.config = new HashMap<String, String>();

		this.instanceTypes = new HashMap<String, InstanceType>();
		this.images = new HashMap<String, Image>();

		this.cloud = cloud;

		String engine = computeXMLNode.getAttributes().getNamedItem("engine")
				.getNodeValue();

		Msg.debug("Compute initialization, engine=" + engine);

		Collection<Host> hosts = new Vector<Host>();
		NodeList nodes = computeXMLNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {

			if (nodes.item(i).getNodeName().compareTo("config") == 0) {
				NamedNodeMap configNNM = nodes.item(i).getAttributes();
				for (int j = 0; j < configNNM.getLength(); j++) {
					this.config.put(configNNM.item(j).getNodeName(),
									configNNM.item(j).getNodeValue());
				}
			}

			if (nodes.item(i).getNodeName().compareTo("cluster") == 0) {
				Msg.info("cluster");
				String id=nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
				String prefix=nodes.item(i).getAttributes().getNamedItem("prefix").getNodeValue();
				String suffix=nodes.item(i).getAttributes().getNamedItem("suffix").getNodeValue();
				String[] radical=nodes.item(i).getAttributes().getNamedItem("radical").getNodeValue().split("-");
				int radStart = Integer.parseInt(radical[0]);
				int radEnd = Integer.parseInt(radical[1]);
				for (int radI=radStart; radI<=radEnd; radI++) {
					String hostId = prefix+radI+suffix;
					try {
						hosts.add(Host.getByName(hostId));
					} catch (HostNotFoundException e) {
						e.printStackTrace();
					}
				}
			}

			if (nodes.item(i).getNodeName().compareTo("host") == 0) {
				try {
					hosts.add(Host.getByName(nodes.item(i).getAttributes().getNamedItem("id").getNodeValue()));
				} catch (HostNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			if (nodes.item(i).getNodeName().compareTo("instance_type") == 0) {
				InstanceType instancetype = new InstanceType(nodes.item(i));
				this.instanceTypes.put(instancetype.getId(), instancetype);
			}

			if (nodes.item(i).getNodeName().compareTo("image") == 0) {
				Image image = new Image(nodes.item(i));
				this.images.put(image.getId(), image);
			}
		}

		try {
			this.computeEngine = (ComputeEngine) Class.forName(engine)
					.getConstructor(Compute.class, Collection.class)
					.newInstance(this, hosts);
		} catch (IllegalArgumentException e) {
			Msg.critical("Something wrong happened while loading the cloud engine "
					+ engine);
			e.printStackTrace();
		} catch (InvocationTargetException e){
			Msg.critical("Something wrong happened while loading the cloud engine "
					+ engine);
			e.printStackTrace();
		} catch (NoSuchMethodException e){
			Msg.critical("Something wrong happened while loading the cloud engine "
					+ engine);
			e.printStackTrace();
		} catch (SecurityException e){
			Msg.critical("Something wrong happened while loading the cloud engine "
					+ engine);
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 * @return the id of this
	 */
	public String getId() {
		return this.cloud.id+"-ComputeEngine";
	}

	/**
	 * 
	 * @return the cloud of this
	 */
	public Cloud getCloud() {
		return this.cloud;
	}

	/**
	 * 
	 * @return the compute engine of this, for admin purpose
	 */
	public ComputeEngine getComputeEngine() {
		return this.computeEngine;
	}
	
	/**
	 * for all properties
	 * 
	 * @param propId
	 *            the id of the property, as is the XML config file
	 * @return the property
	 * @throws Exception 
	 */
	public String getConfig(String propId) throws MissingConfigException {
		String res = this.config.get(propId);
		if ( res != null)
		{
			return res;
		}
		else
		{
			throw new MissingConfigException(this.cloud.getId(),"compute",propId);
		}
	}

	/**
	 * 
	 * @return all the instances, alive and terminated, of this
	 */
	public Collection<Instance> describeInstances() {
		return this.instances.values();
	}

	/**
	 * 
	 * @param id
	 *            the id of one instance
	 * @return the instance of the id
	 */
	public Instance describeInstance(String id) {
		return this.instances.get(id);
	}

	/**
	 * 
	 * @return all the types of instance available on this
	 */
	public Collection<InstanceType> describeInstanceTypes() {
		return this.instanceTypes.values();
	}

	/**
	 * 
	 * @param id
	 *            the id of one type of instance
	 * @return the type of instance of the id
	 */
	public InstanceType describeInstanceType(String id) {
		return this.instanceTypes.get(id);
	}

	/**
	 * 
	 * @return all the images available on this
	 */
	public Map<String, Image> describeImages() {
		return this.images;
	}

	/**
	 * 
	 * @param id
	 *            the id of one image
	 * @return the image of the id
	 */
	public Image describeImage(String id) {
		return this.images.get(id);
	}

	/**
	 * Run a new instance, using a simple round-robin scheduling
	 * 
	 * @param imageId
	 *            the id of the image of the instance.
	 * @param instanceTypeId
	 *            the id of the type of the instance
	 *            or null if no instance can be provisioned
	 * @return the id of the instance, about to be started
	 */
	public String runInstance(String imageId, String instanceTypeId) {
		Instance instance = this.computeEngine.newInstance(
				this.getCloud().getId() + "-" + String.format("%03d", instancesId),
				this.images.get(imageId),
				this.instanceTypes.get(instanceTypeId));
		
		if (instance == null) return null;
		
		this.computeEngine.doCommand(ComputeEngine.COMMAND.START, instance);


		this.instances.put(instance.id, instance);
		instancesId++;
		return instance.id;
	}

	/**
	 * Run a new instance
	 * 
	 * @param imageId
	 *            the id of the image of the instances to run.
	 * @param instanceTypeId
	 *            the id of the type of the instances to run
	 * @param nInstances
	 *            the number of instances to run
	 * @return an array containing the ids of the instances that will be actually started. 
	 */
	public String[] runInstances(String imageId, String instanceTypeId, int nInstances) {
		Vector<String> instancesId = new Vector<String>();
		for (int i=0; i<nInstances; i++) {
			String instanceId = runInstance(imageId, instanceTypeId);
			if (instanceId == null) break;
			instancesId.add(instanceId);
		}

		return instancesId.toArray(new String[0]);
	}

	/**
	 * Terminate a given instance.
	 * 
	 * @param instanceId
	 *            The id of the instance to be be terminated.
	 */
	public void terminateInstance(String instanceId) {
		if (this.instances.get(instanceId).isTerminating == true) return;
		
		this.instances.get(instanceId).isTerminating = true;
		this.computeEngine.doCommand(ComputeEngine.COMMAND.SHUTDOWN,this.instances.get(instanceId));
	}

	/**
	 * Suspend a given instance.
	 * 
	 * @param instanceId
	 *            The id of the instance to be be suspended.
	 * TODO check why it isn't working
	 */
	public void suspendInstance(String instanceId) {
		this.computeEngine.doCommand(ComputeEngine.COMMAND.SUSPEND,
				this.instances.get(instanceId));
	}

	/**
	 * Resume a given instance.
	 * 
	 * @param instanceId
	 *            The if of the instance to be be resumed.
	 */
	public void resumeInstance(String instanceId) {
		this.computeEngine.doCommand(ComputeEngine.COMMAND.RESUME,
				this.instances.get(instanceId));
	}

	/**
	 * Give the current availability of new instances on this.
	 * 
	 * @param instanceTypeId
	 *            The id of a type of instance
	 * @return The number of available instances of the given type
	 */
	public int describeAvailability(String instanceTypeId) {
		return this.computeEngine.describeAvailability(
				this.instanceTypes.get(instanceTypeId));
	}
	
	/**
	 * Terminate this, by terminating all instances and then the engine
	 * 
	 * @throws HostFailureException
	 */
	public void terminate() throws HostFailureException {
		Msg.verb("Terminating all remaining instances");
		for (Instance instance : this.instances.values()) {
			terminateInstance(instance.id);
		}
		this.computeEngine.terminate();
	}
}
