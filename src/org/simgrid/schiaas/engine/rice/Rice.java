package org.simgrid.schiaas.engine.rice;

import java.util.Collection;
import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.ComputeEngine;

/**
 * Reduced Implementation of Compute Engine.
 * 
 * @author julien
 */
public class Rice extends ComputeEngine {


	/** Host of the controller of this. */
	protected Host controller;

	/** All the host of this. */
	protected Vector<RiceHost> riceHosts;

	/** Storage used for the images. */
	protected Storage imgStorage;

	/**
	 * Enumerates the possible image caching strategies.
	 * @author julien
	 */
	protected static enum IMGCACHING {
		ON, OFF, PRE
	};

	/** the caching strategy */
	protected IMGCACHING imgCaching;

	/** the delay between two consecutive boots */
	protected double interBootDelay;
	
	/**
	 * Constructor
	 * 
	 * @param compute
	 * @param hosts
	 */
	public Rice(Compute compute, Collection<Host> hosts) {
		super(compute, hosts);

		try {
			this.controller = Host.getByName((String) compute.getConfig("controller"));
		} catch (HostNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.riceHosts = new Vector<RiceHost>();
		for (Host host : hosts) {
			this.riceHosts.add(new RiceHost(host));
		}
		
		Msg.info("storing images");
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

	/**
	 * Select one host to run a new instance of a given type. Basically search
	 * for one host having enough available cores to satisfy the type of
	 * instance.
	 * 
	 * @param instanceType
	 *            The type of instance.
	 * @return One host able to run an instance of this type. TODO (or not)
	 *         Generalize this by an abstraction to be able too plug any
	 *         strategy.
	 */
	public RiceHost assignVM(InstanceType instanceType) {
		int i = 0;
		double core = Double.parseDouble(instanceType.getProperty("core"));
		while (i < this.riceHosts.size()
				&& (this.riceHosts.get(i).host.getCore() - this.riceHosts
						.get(i).coreUsedByVMcount) < core) {
			Msg.info("assign " + i + "/" + this.riceHosts.size() + " : " + core
					+ "-" + this.riceHosts.get(i).coreUsedByVMcount + "/"
					+ this.riceHosts.get(i).host.getCore());
			i++;
		}

		if (i == this.riceHosts.size())
			return null;
		return this.riceHosts.get(i);
	}

	public int describeAvailability(InstanceType instanceType) {
		double core = Double.parseDouble(instanceType.getProperty("core"));
		int availability = 0;
		for (int i = 0; i < this.riceHosts.size(); i++)
			availability += (this.riceHosts.get(i).host.getCore() - this.riceHosts
					.get(i).coreUsedByVMcount) / core;

		return availability;
	}

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

	public void terminate() {
	}

	@Override
	public Instance newInstance(String id, Image image, InstanceType instanceType) {
		RiceHost riceHost = assignVM(instanceType);
		//RiceHost riceHost = this.riceHosts.get(0);
		if (riceHost == null)
			return null;
		return new RiceInstance(id, image, instanceType, riceHost);
	}
}
