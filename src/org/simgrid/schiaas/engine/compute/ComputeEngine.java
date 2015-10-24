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

	
	/**
	 * Enumerates the possible commands to control instances.
	 * @author julien
	 */
	public static enum COMMAND {
		START, SHUTDOWN, SUSPEND, RESUME, REBOOT
	};

	/**
	 * Constructor with scheduler.
	 * 
	 * @param compute
	 *            The compute of this.
	 * @param hosts
	 *            All of the host of this.
	 * @param computeScheduler
	 * 			  The scheduler of this.
	 * TODO delete host of kept into Compute
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
	 * @param computeScheduler the scheduler to use in this compute.
	 */
	public void setComputeScheduler(String schedulerName, Map<String, String> config){
		if (this.computeScheduler != null)
			this.computeScheduler.terminate();
		
		this.computeScheduler = ComputeScheduler.load(schedulerName, this, config);;
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
	 */
	public abstract Instance newInstance(String id, Image image, InstanceType instanceType);

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
	 * @param instance
	 *            The instance to migrate
	 * @param computeHost
	 *            The host to migrate the instance to
	 */
	public abstract void liveMigration(Instance instance, ComputeHost computeHost) throws HostFailureException;

	/**
	 * Migrate an instance to one host chose by the engine
	 * 
	 * @param instance
	 *            The instance to migrate
	 *            
	 * @return The host to which the instance is migrating
	 */
	public abstract ComputeHost liveMigration(Instance instance) throws HostFailureException;
		
	/**
	 * Terminate this.
	 */
	public void terminate() {
		if (this.computeScheduler != null)
			this.computeScheduler.terminate();
	}

}
