package org.simgrid.schiaas.engine.compute.scheduler.simplescheduler;

import java.util.Collection;
import java.util.Map;

import org.simgrid.msg.Msg;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeScheduler;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeHost;
import org.simgrid.schiaas.exceptions.VMSchedulingException;


/**
 * This scheduler selects the host with the greatest weight.
 */
public class SimpleScheduler extends ComputeScheduler {
	
	/**
	 * Enumerates the possible scheduler types
	 */
	protected static enum TYPE {
		BALANCER, CONSOLIDATOR;
	};
	
	protected TYPE type;
	
	public SimpleScheduler(ComputeEngine computeEngine, Map<String, String> config) {
		super(computeEngine, config);

		try {
			this.type = TYPE.valueOf(config.get("type").toUpperCase());
		} catch (IllegalArgumentException e) {
			Msg.critical("The type of scheduling '"+config.get("type")+"' is not supported by the SimpleScheduler");
			e.printStackTrace();
		}
	}

	/**
	 * Compute the weight of one compute host.
	 * The greatest one will be selected to host the VMs. 
	 * @param computeHost the compute host to evaluate
	 * @param instanceType the type of the instance to be scheduled.
	 * @return Return the weight of the given host depending on the number of free cores.
	 */
	public double getWeight(ComputeHost computeHost, InstanceType instanceType) {
		
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
	 * @return the ComputeHost onto which the VM will be scheduled.
	 * @throws VMSchedulingException whenever no suitable host is found
	 */
	@Override
	public ComputeHost schedule(InstanceType instanceType) throws VMSchedulingException {
		Collection<ComputeHost> computeHosts = computeEngine.getComputeHosts();
		
		ComputeHost result = null;
		double resultWeight = -Double.MAX_VALUE;
		
		for (ComputeHost ch : computeHosts) {
			if ( ch.isAvailable() && ch.canHost(instanceType) > 0 )	{
				double weight = getWeight(ch, instanceType);
				//Msg.info(" - Weigth of "+ch.getHost().getName()+" = "+weight);
				if ( weight > resultWeight ) {
					result = ch;
					resultWeight = weight;
				}
			}
		}
		
		if (result == null) {
			throw(new VMSchedulingException(this, instanceType, "no suitable host was found")); 
		}

		return result;
	}
	
	@Override
	public void terminate() {
	}
	
}
