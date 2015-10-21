package org.simgrid.schiaas.engine.compute.scheduler.simplescheduler;

import java.util.Collection;
import java.util.Map;

import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeScheduler;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeHost;


/**
 * This scheduler selects the host with the greatest weight.
 */
public abstract class SimpleScheduler extends ComputeScheduler {
	
	/**
	 * Enumerates the possible scheduler types
	 */
	protected static enum TYPE {
		BALANCER, CONSOLIDATOR;
	};
	
	protected TYPE type;
	
	public SimpleScheduler(ComputeEngine computeEngine, Map<String, String> config) {
		super(computeEngine, config);
		
		this.type = TYPE.valueOf(config.get("type").toUpperCase());
	}

	/**
	 * Compute the weight of one compute host.
	 * The greatest one will be selected to host the VMs. 
	 * @param computeHost the compute host to evaluate
	 * @param instanceType the type of the instance to be scheduled.
	 * @return the weight of the candidate host, 0 if not suitable.
	 */
	protected double getWeight(ComputeHost computeHost, InstanceType instanceType) {
		
		switch (type) {
		case BALANCER: 
			return computeHost.getFreeCores();
		case CONSOLIDATOR:
			return -computeHost.getFreeCores();
		default:
			return 0;
		}
		
	}

	
	/**
	 * Simply compute a weight for each PMs, and select the greatest one.
	 * Only check for cores availability.
	 * Return null if the selected host is not suitable for the instanceType.  
	 */
	@Override
	public ComputeHost schedule(InstanceType instanceType) {
			
		Collection<ComputeHost> computeHosts = computeEngine.getComputeHosts();
		
		ComputeHost result = null;
		double resultWeight = 0;
		
		for (ComputeHost ch : computeHosts)  {
			
			if ( ch.canHost(instanceType) > 0 )
			{
				double weight = getWeight(ch, instanceType);
				if ( weight > resultWeight ) {
					result = ch;
					resultWeight = weight;
				}
			}
		}
		
		return result;
	}
	
}
