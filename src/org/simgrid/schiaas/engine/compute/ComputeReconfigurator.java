package org.simgrid.schiaas.engine.compute;

import java.util.Map;

import org.simgrid.msg.Msg;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.exceptions.MissingConfigException;
import org.simgrid.schiaas.exceptions.VMSchedulingException;
import org.simgrid.schiaas.tools.Trace;

/**
 * Describes an abstract reconfigurator, to decide where instances must be migrated at runtime.
 * @author julien.gossa@unistra.fr
 *
 */
public abstract class ComputeReconfigurator {
	
	/** computeEngine of this **/
	protected ComputeEngine computeEngine;
	
	/** Contains all the config properties of this scheduler. */
	protected Map<String, String> config;
	
	/** Tracing */
	protected Trace trace;
	
	/**
	 * Constructor.
	 * @param computeEngine the compute engine using this scheduler
	 * @param config the configuration of this scheduler
	 */
	public ComputeReconfigurator(ComputeEngine computeEngine, Map<String, String> config) {
		this.computeEngine = computeEngine;
		this.config = config;
		
		trace = computeEngine.getCompute().getTrace().newSubTrace("reconfigurator");
		trace.addProperties(config);
	}
	
	/**
	 * for all properties
	 * 
	 * @param propId
	 *            the id of the property, as is the XML config file
	 * @return the property
	 * @throws MissingConfigException
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
	 * @return the compute engine
	 */
	public ComputeEngine getComputeEngine() {
		return this.computeEngine;
	}
	
	/**
	 * Load a reconfigurator class 
	 * @param reconfiguratorName the name of the class
	 * @param computeEngine the engine of the compute using this reconfigurator.
		 * @param config the configuration of this reconfigurator 
	 * @return an <i>ComputeReconfigurator</i> object
	 */
	public static ComputeReconfigurator load(String reconfiguratorName, ComputeEngine computeEngine, Map<String, String> config) {
		ComputeReconfigurator computeReconfigurator = null;
		
		try {
			computeReconfigurator = (ComputeReconfigurator)Class.forName(reconfiguratorName).getConstructor(ComputeEngine.class, Map.class).newInstance(computeEngine, config);
		} catch (Exception e) {
			Msg.critical("Something wrong happened while loading the reconfigurator "
					+ reconfiguratorName);
			e.printStackTrace();
		}	
		
		return computeReconfigurator;
	}
	
	/**
	 * Called at the termination of the cloud
	 */
	public void terminate() {
	}
	
	/**
	 * Heuristics for reconfiguration
	 * @author julien
	 *
	 */
	public static abstract class ReconfigurationHeuristic {
		public ReconfigurationHeuristic(ComputeEngine computeEngine, Map<String, String> config) {
		}
		public abstract void computeReconfigurationPlan();
		public abstract void applyReconfigurationPlan();
	}
}
