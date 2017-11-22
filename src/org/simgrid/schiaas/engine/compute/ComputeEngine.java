package org.simgrid.schiaas.engine.compute;

import java.util.List;
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.exceptions.VMSchedulingException;


/**
 * Interface for ComputeEngine: internals to manage the instances lifecycle
 * 
 * @author julien.gossa@unistra.fr
 */
public abstract class ComputeEngine {

	/** The compute of this */
	protected Compute compute;

	/** The scheduler of this */
	protected ComputeScheduler computeScheduler;

	/** The reconfigurator of this */
	protected ComputeReconfigurator computeReconfigurator;
	
	/**
	 * Enumerates the possible commands to control instances.
	 * @author julien.gossa@unistra.fr
	 */
	public static enum COMMAND {
		START, SHUTDOWN, SUSPEND, RESUME, REBOOT
	};

	/**
	 * Constructor without scheduler.
	 * 
	 * @param compute The compute of this.
	 * @param hosts All of the host of this.
	 */
	public ComputeEngine(Compute compute, List<Host> hosts) {
		this.compute = compute;
		this.computeScheduler = null;
		
		Msg.debug("Compute engine initialization");
	}

	/**
	 * @return The compute of this.
	 */
	public Compute getCompute() {
		return this.compute;
	}

	/**
	 * Set and start the scheduler
	 * @param schedulerName the class of the scheduler
	 * @param config the configuration of the scheduler
	 */
	public void setComputeScheduler(String schedulerName, Map<String, String> config){		
		this.computeScheduler = ComputeScheduler.load(schedulerName, this, config);
	}
	
	/**
	 * Set and start the reconfigurator
	 * @param reconfiguratorName the class of the reconfigurator
	 * @param config the configuration of the reconfigurator
	 */
	public void setComputeReconfigurator(String reconfiguratorName, Map<String, String> config){		
		this.computeReconfigurator = ComputeReconfigurator.load(reconfiguratorName, this, config);
	}
	
	/**
	 * @return The hosts of this
	 */
	public abstract List<Host> getHosts();

	/**
	 * @return The computeHosts of this.
	 */
	public abstract List<ComputeHost> getComputeHosts();	
	
	/**
	 * @param hostName an host name
	 * @return the corresponding ComputeHost
	 */
	public abstract ComputeHost getComputeHostByName(String hostName);
	
	/**
	 * @param instance an instance
	 * @return the ComputeHost hosting the instance
	 */
	public abstract ComputeHost getComputeHostOf(Instance instance);


	/**
	 * Return a new instance, because the id is set here (for homogeneity),
	 * while the actual class of instance is only known at the actual engine
	 * level.
	 * 
	 * @param id
	 *            The id if the instance
	 * @param image
	 *            The image of the instance
	 * @param instanceType
	 *            The type of the instance
	 * @return A new instance
	 * @throws VMSchedulingException When the scheduler fails to schedule the instance
	 */
	public abstract Instance newInstance(String id, Image image, InstanceType instanceType) throws VMSchedulingException;

	/**
	 * 
	 * @param instanceType
	 *            One type of instance.
	 * @return The number of available instances of the given type.
	 */
	public abstract int describeAvailability(InstanceType instanceType);

	/**
	 * This method implements implementation specific command logic
	 * 
	 * @param command
	 *            The command to execute
	 * @param instance
	 *            The target instance
	 */
	public abstract void doCommand(COMMAND command, Instance instance);

	/**
	 * Migrate an instance to one given ComputeHost
	 * 
	 * @param instance The instance to migrate
	 * @param computeHost The host to migrate the instance to
	 * @throws HostFailureException
	 */
	public abstract void liveMigration(Instance instance, ComputeHost computeHost) throws HostFailureException;

	/**
	 * Migrate an instance to one host chose by the engine
	 * 
	 * @param instance The instance to migrate
	 * @return The host to which the instance is migrating
	 * @throws VMSchedulingException When the scheduler fails to schedule the instance
	 * @throws HostFailureException
	 */
	public abstract ComputeHost liveMigration(Instance instance) throws HostFailureException, VMSchedulingException;
		
	/**
	 * Terminate this.
	 */
	public void terminate() {
		Msg.debug("Terminating the compute engine");
		if (this.computeScheduler != null) {
			this.computeScheduler.terminate();
		}
		if (this.computeReconfigurator != null) {
			this.computeReconfigurator.terminate();
		}
	}

}
