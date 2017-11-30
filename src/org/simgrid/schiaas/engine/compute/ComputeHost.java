package org.simgrid.schiaas.engine.compute;

import java.util.Collection;
import org.simgrid.msg.Host;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;

/**
 * Host at Compute level, with cloud admin-level information.
 * @author julien.gossa@unistra.fr
 */
public interface ComputeHost {
	
	/**
	 * @return All the hosted instances
	 */
	public abstract Collection<Instance> getHostedInstances();
	
	/** 
	 * @return the physical host
	 */
	public abstract Host getHost();

	/**
	 * Set the availability to host new instances
	 * @param availability true if available to host new instances
	 */
	public abstract void setAvailability(boolean availability);
	
	/**
	 * @return true if available to host new instances 
	 */	
	public abstract boolean isAvailable();

	/**
	 * @param instanceType test if this host can host an instance of this type.
	 * @return the number of instances of a type this can host. 
	 */	
	public abstract int canHost(InstanceType instanceType);
	
	/**
	 * @return the amount of free cores to host new VMs
	 */
	public abstract int getFreeCores();

	/**
	 * @return the amount of RAM in B to host new VMs
	 */
	public abstract int getRam();
	
	/**
	 * @return the amount of free RAM in B to host new VMs
	 */
	public abstract int getFreeRam();
	
	/**
	 * @return the amount of free disk to host new VMs
	 */
	public abstract double getFreeDisk();
	
	/**
	 * @return true if some instance SLA is violated on this host
	 */
	public abstract boolean isSLAViolated();
	
}
