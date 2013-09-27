package org.simgrid.schiaas.engine.rice;

import java.util.Collection;
import java.util.Vector;

import org.simgrid.schiaas.engine.ComputeEngine;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.Instance;
import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Process;

/**
 * Reduced Implementation of Compute Engine.
 * @author julien
 */
public class Rice extends ComputeEngine {

	/** Host of the controller of this. */
	protected Host controller;
	
	/** All the host of this. */
	protected Vector<RiceHost> riceHosts;
	
	/**
	 * Constructor
	 * @param compute
	 * @param hosts
	 */
	public Rice(Compute compute, Collection<Host> hosts) {
		super(compute, hosts);
		
		try {
			controller = Host.getByName((String)compute.getProperty("controller"));
		} catch (HostNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.riceHosts = new Vector<RiceHost>();
		for (Host host : hosts) {
			riceHosts.add(new RiceHost(host));
		}		
	}
	
	/**
	 * Select one host to run a new instance of a given type.
	 * Basically search for one host having enough available cores to satisfy the type of instance.
	 * @param instanceType The type of instance.
	 * @return One host able to run an instance of this type.
	 * TODO (or not) Generalize this by an abstraction to be able too plug any strategy.
	 */
	public RiceHost assignVM(InstanceType instanceType) {
		int i = 0;
		double core = Double.parseDouble(instanceType.getProperty("core"));
		while (i<riceHosts.size() && (riceHosts.get(i).host.getCore()-riceHosts.get(i).coreUsedByVMcount) < core ) {
			Msg.info("assign "+i+"/"+riceHosts.size()+" : "+core +"-"+ riceHosts.get(i).coreUsedByVMcount+"/"+riceHosts.get(i).host.getCore());	
			i++;
		}
		
		if (i == riceHosts.size()) return null;
		return riceHosts.get(i);
	}
	
	
	public int describeAvailability(InstanceType instanceType) {
		double core = Double.parseDouble(instanceType.getProperty("core"));
		int availability = 0;
		for (int i=0; i<riceHosts.size(); i++)
			availability += (riceHosts.get(i).host.getCore()-riceHosts.get(i).coreUsedByVMcount) / core;
		
		return availability;
	}
	
	public void doCommand(COMMAND command, Instance instance) {
		RiceInstance riceInstance = (RiceInstance) instance;
		switch (command) {
		case START :
			riceInstance.start();
			break;
		case SHUTDOWN :
			riceInstance.shutdown();
			riceInstance.destroy();
			break;
		case SUSPEND :
			riceInstance.suspend();
			break;
		case RESUME :
			riceInstance.resume();
			break;
		case REBOOT :
			riceInstance.off();
			riceInstance.on();
			break;
		}
	}

	public void terminate() {
	}

	@Override
	public Instance newInstance(String id, Image image,	InstanceType instanceType) {
		RiceHost riceHost = assignVM(instanceType);
		if (riceHost == null) return null;
		
		return new RiceInstance(id, image, instanceType, riceHost);
	}
}

