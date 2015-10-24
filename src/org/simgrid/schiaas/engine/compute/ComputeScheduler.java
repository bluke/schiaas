package org.simgrid.schiaas.engine.compute;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.simgrid.msg.Msg;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.exceptions.MissingConfigException;

/**
 * Describes a abstract scheduler, to decide where new instances must be run, and enforce reconfiguration plans.
 * @author julien.gossa@unistra.fr
 *
 */
public abstract class ComputeScheduler {
	
	/** computeEngine of this **/
	protected ComputeEngine computeEngine;
	
	/** Contains all the config properties of this scheduler. */
	protected Map<String, String> config;
		
	/**
	 * Constructor.
	 * @param computeEngine the compute engine using this scheduler
	 * @param config the configuration of this scheduler
	 */
	public ComputeScheduler(ComputeEngine computeEngine, Map<String, String> config) {
		this.computeEngine = computeEngine;
		this.config = config;
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
		if ( res == null)
		{
			throw new MissingConfigException(computeEngine.getCompute().getCloud(),"scheduler",propId);
		}
		
		return res;
	}

	
	/**
	 * 
	 * @param instanceType the type of the instance to be scheduled
	 * @return the ComputeHost to host the instance, null if no host is available.
	 */
	public abstract ComputeHost schedule(InstanceType instanceType);

	
    /**
     * Load a scheduler class 
     * @param schedulerName the name of the class
     * @param computeEngine the engine of the compute using this scheduler.
	 * @param config the configuration of this scheduler 
     * @return an <i>ComputeScheduler</i> object
     */
	public static ComputeScheduler load(String schedulerName, ComputeEngine computeEngine, Map<String, String> config) {
		ComputeScheduler computeScheduler = null;
		
		try {
			computeScheduler = (ComputeScheduler)Class.forName(schedulerName).getConstructor(ComputeEngine.class, Map.class).newInstance(computeEngine, config);
		} catch (Exception e) {
			Msg.critical("Something wrong happened while loading the scheduler "
					+ schedulerName);
			e.printStackTrace();
		}	
		
		return computeScheduler;
    }

	public void terminate() {
	}
}