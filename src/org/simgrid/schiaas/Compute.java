/*
 * Controller
 *
 * Copyright 2006-2013 The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package org.simgrid.schiaas;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.simgrid.msg.*;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
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
	 */
	protected Compute(Cloud cloud, Node computeXMLNode) {

		Msg.debug("Compute initialization of the cloud : " + cloud.getId());
		
		this.instances = new HashMap<String, Instance>();
		this.instancesId = 0;
		
		this.config = new HashMap<String, String>();

		this.instanceTypes = new HashMap<String, InstanceType>();
		this.images = new HashMap<String, Image>();

		this.cloud = cloud;

		String engine = computeXMLNode.getAttributes().getNamedItem("engine")
				.getNodeValue();
		
		Msg.debug("Compute initialization, engine=" + engine);
		
		List<Host> hosts = new Vector<Host>();
		NodeList nodes = computeXMLNode.getChildNodes();

		String schedulerName = null;
		HashMap<String, String> schedulerConfig = new HashMap<String, String>();

		for (int i = 0; i < nodes.getLength(); i++) {

			if (nodes.item(i).getNodeName().compareTo("config") == 0) {
				NamedNodeMap configNNM = nodes.item(i).getAttributes();
				for (int j = 0; j < configNNM.getLength(); j++) {
					this.config.put(configNNM.item(j).getNodeName(),
									configNNM.item(j).getNodeValue());
				}
			}

			if (nodes.item(i).getNodeName().compareTo("scheduler") == 0) {
				schedulerName = "undefined";
				NamedNodeMap configNNM = nodes.item(i).getAttributes();
				for (int j = 0; j < configNNM.getLength(); j++) {
					schedulerConfig.put(configNNM.item(j).getNodeName(),
										configNNM.item(j).getNodeValue());
				}
				schedulerName = schedulerConfig.remove("name"); 
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
					.getConstructor(Compute.class, List.class)
					.newInstance(this, hosts);
		} catch (Exception e) {
			Msg.critical("Something wrong happened while loading the cloud engine "
					+ engine);
			e.printStackTrace();
		}
		if (schedulerName != null) {
			this.computeEngine.setComputeScheduler(schedulerName, schedulerConfig);
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
	 * Getter for all configuration properties
	 * 
	 * @param propId
	 *            the id of the property, as is the XML config file
	 * @return the property
	 * @throws Exception 
	 */
	public String getConfig(String propId) throws MissingConfigException {
		String res = this.config.get(propId);
		if ( res == null)
		{
			throw new MissingConfigException(this.cloud,"compute",propId);
		}
		
		return res;
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
	 * @return the instance, about to be started
	 */
	public Instance runInstance(String imageId, String instanceTypeId) {
		Instance instance = this.computeEngine.newInstance(
				this.getCloud().getId() + "-" + String.format("%03d", instancesId),
				this.images.get(imageId),
				this.instanceTypes.get(instanceTypeId));
		
		if (instance == null) return null;
		
		this.computeEngine.doCommand(ComputeEngine.COMMAND.START, instance);


		this.instances.put(instance.id, instance);
		instancesId++;
		return instance;
	}

	/**
	 * Run several new instances
	 * 
	 * @param imageId
	 *            the id of the image of the instances to run.
	 * @param instanceTypeId
	 *            the id of the type of the instances to run
	 * @param nInstances
	 *            the number of instances to run
	 * @return a collection of the instances, about to be started 
	 */
	public Collection<Instance> runInstances(String imageId, String instanceTypeId, int nInstances) {
		Collection<Instance> instances = new Vector<Instance>();
		for (int i=0; i<nInstances; i++) {
			Instance instance = runInstance(imageId, instanceTypeId);
			if (instance == null) break;
			instances.add(instance);
		}

		return instances;
	}

	/**
	 * Terminate a given instance.
	 * 
	 * @param instanceId
	 *            The id of the instance to be be terminated.
	 */
	public void terminateInstance(String instanceId) {
		this.instances.get(instanceId).terminate();
	}

	/**
	 * Terminate a given instance.
	 * 
	 * @param instance
	 *            The instance to be be terminated.
	 */
	public void terminateInstance(Instance instance) {
		instance.terminate();
	}

	
	/**
	 * Suspend a given instance.
	 * 
	 * @param instanceId
	 *            The id of the instance to be be suspended.
	 */
	public void suspendInstance(String instanceId) {
		this.instances.get(instanceId).suspend();
	}

	/**
	 * Suspend a given instance.
	 * 
	 * @param instance
	 *            The instance to be be suspended.
	 */
	public void suspendInstance(Instance instance) {
		instance.suspend();
	}

	
	/**
	 * Resume a given instance.
	 * 
	 * @param instanceId The if of the instance to be be resumed.
	 */
	public void resumeInstance(String instanceId) {
		this.instances.get(instanceId).resume();
	}

	/**
	 * Resume a given instance.
	 * 
	 * @param instance The instance to be be resumed.
	 */
	public void resumeInstance(Instance instance) {
		instance.resume();
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
			if (! instance.isTerminating) {
				terminateInstance(instance.id);
			}
		}
		this.computeEngine.terminate();
	}
}
