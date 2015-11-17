package org.simgrid.schiaas.engine.compute.rice;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeHost;
import org.simgrid.schiaas.exceptions.MissingConfigException;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

/**
 * Reduced Implementation of Compute Engine.
 * Simple management of the instances lifecycle.
 * 
 * @author julien.gossa@unistra.fr
 */
public class Rice extends ComputeEngine {
		
	/** Host of the controller of this. */
	protected Host controller;

	/** All the host of this. */
	private Vector<Host> hosts;	
	
	/** All the computeHost of this. */
	protected Vector<ComputeHost> riceHosts;

	/**
	 * Enumerates the possible off-load types.
	 */
	protected static enum OFFLOADTYPE {
		SEQUENTIAL, PARALLEL
	};

	/** the off-load type */
	protected OFFLOADTYPE offLoadType;
	
	
	/** Storage used for the images. */
	protected Storage imgStorage;

	/**
	 * Enumerates the possible image caching strategies.
	 */
	protected static enum IMGCACHING {
		ON, OFF, PRE
	};

	/** the caching strategy */
	protected IMGCACHING imgCaching;

	/** the delay between two consecutive boots */
	protected double interBootDelay;
	
	/**
	 * Constructor.
	 * 
	 * @param compute The compute of this engine.
	 * @param hosts All of the host of this engine.
	 * @throws MissingConfigException Thrown whenever one required configuration parameter is not found.
	 * @throws HostNotFoundException Thrown whenever one given host is not found. 
	 * @throws FileNotFoundException 
	 */
	public Rice(Compute compute, List<Host> hosts) throws MissingConfigException, HostNotFoundException, FileNotFoundException {
		super(compute, hosts);
		
		try{
			compute.getConfig("controller");
			compute.getConfig("image_storage");
			compute.getConfig("image_caching");
			compute.getConfig("inter_boot_delay");
		} catch (MissingConfigException e)
		{
			Msg.critical(e.getMessage());
			throw e;
		}		

		// retrieving the controller
		try {
			this.controller = Host.getByName((String) compute.getConfig("controller"));
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened in RISE while trying to locate its controller: "+compute.getConfig("controller"));
			e.printStackTrace();
		} catch (Exception e){
			// TODO Anything clever to do here?
		}

		// retrieving the hosts
		this.riceHosts = new Vector<>();

		for (Host host : hosts) {
			this.riceHosts.add(new RiceHost(this,host));
		}
		
		// retrieving the images
		Msg.verb("RICE: storing images");
		imgStorage = compute.getCloud().getStorage(compute.getConfig("image_storage"));
		for (Image image : compute.describeImages().values()) {
			Data imgData = new Data("RICEIMG-"+image.getId(), Double.parseDouble(image.getProperty("size")));
			try {
				imgStorage.putInstantaneously(imgData);
			} catch (Exception e) {
				Msg.critical("Something bad happens when trying to store the images of RICE");
				e.printStackTrace();
			}
		}

		imgCaching = IMGCACHING.valueOf(compute.getConfig("image_caching"));
		interBootDelay = Double.parseDouble(getCompute().getConfig("inter_boot_delay"));
	}

	@Override
	public List<Host> getHosts() {
		return hosts;
	}

	@Override
	public List<ComputeHost> getComputeHosts() {
		return riceHosts;
	}
	
	@Override
	public ComputeHost getComputeHostByName(String hostName) {
		for (ComputeHost ch : riceHosts)
			if (ch.getHost().getName().equals(hostName))
				return ch;
		return null;
	}

	/**
	 * Checks the amount of instances of one given type that can be ran on this Compute.
	 * @param instanceType A type of instance
	 * @return The amount of instances of this type that can be ran
	 */
	@Override
	public int describeAvailability(InstanceType instanceType) {
		double core = Double.parseDouble(instanceType.getProperty("core"));
		int availability = 0;
		for (ComputeHost riceHost : this.riceHosts) {
			availability += (((RiceHost) riceHost).freeCores) / core;
		}

		return availability;
	}

	/**
	 * Executes one command.
	 * @param command The command to execute.
	 * @param instance The instance concerned by the command.
	 */
	@Override
	public void doCommand(COMMAND command, Instance instance) {
		RiceInstance riceInstance = (RiceInstance) instance;

		RiceControllerProcess rcp = new RiceControllerProcess(this, command, riceInstance);
		RiceNodeProcess rnp = new RiceNodeProcess(this, riceInstance.riceHost);
		
		try {
			rcp.start();			
			rnp.start();
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened in RISE while trying to execute a command");
			e.printStackTrace();
		}
	}
		
	
	/**
	 * Migrate an instance to one given host
	 * 
	 * @param instance The instance to migrate
	 * @param destination The compute host to migrate the instance to
	 * @throws HostFailureException when the migration fails at the simgrid level
	 */
	@Override
	public void liveMigration(Instance instance, ComputeHost destination) throws  HostFailureException {
		RiceInstance riceInstance = (RiceInstance) instance;		
	
		Msg.verb("live migration: "+riceInstance.getId()+" to "+destination.getHost().getName());
	
		((RiceHost) destination).addInstance(riceInstance);
		riceInstance.vm().migrate(destination.getHost());
		riceInstance.riceHost.removeInstance(riceInstance);
		riceInstance.riceHost = (RiceHost) destination;
	}

	/**
	 * Migrate an instance to one host chosen by RICE.
	 * 
	 * @param instance The instance to migrate
	 * @return the host choosen by RICE, or null whenever there was no suitable host available.
	 * @throws HostFailureException
	 * @throws VMSchedulingException when the scheduler fails
	 */
	@Override
	public ComputeHost liveMigration(Instance instance) throws HostFailureException, VMSchedulingException {
		RiceInstance riceInstance = (RiceInstance) instance;
		
		ComputeHost riceHost = (RiceHost) computeScheduler.schedule(riceInstance.getInstanceType());
		if (riceHost == null || riceHost == riceInstance.getHost()) return null;
		
		liveMigration(instance, riceHost);
		
		return riceHost;
	}

	/**
	 * Selects one physical host and creates a new instance.
	 * @return The newly created instance, or null if no suitable physical host can be found.
	 * @throws VMSchedulingException When the scheduler fails to schedule the instance
	 */
	@Override
	public Instance newInstance(String id, Image image, InstanceType instanceType) throws VMSchedulingException {
		RiceHost riceHost = (RiceHost) computeScheduler.schedule(instanceType);
		
		if (riceHost == null) return null;
		return new RiceInstance(compute, id, image, instanceType, riceHost);
	}

	@Override
	public ComputeHost getComputeHostOf(Instance instance) {
		return ((RiceInstance)instance).riceHost;
	}
	
	@Override
	public void terminate() {
		super.terminate();
	}
	
}
