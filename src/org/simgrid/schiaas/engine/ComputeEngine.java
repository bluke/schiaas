package org.simgrid.schiaas.engine;

import java.util.Collection;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
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
	
	/** The physical hosts used to run instances */
	protected Collection<Host> hosts;

	/**
	 * Enumerates the possible commands to control instances.
	 * 
	 * @author julien
	 */
	public static enum COMMAND {
		START, SHUTDOWN, SUSPEND, RESUME, REBOOT
	};

	/**
	 * Unique constructor.
	 * 
	 * @param compute
	 *            The compute of this.
	 * @param hosts
	 *            All of the host of this.
	 * TODO delete host of kept into Compute
	 */
	public ComputeEngine(Compute compute, Collection<Host> hosts) {
		this.compute = compute;
		this.hosts = hosts;
	}

	/**
	 * @return The compute of this.
	 */
	public Compute getCompute() {
		return this.compute;
	}

	/**
	 * @return The hosts of this
	 */
	public Collection<Host> getHosts() {
		return this.hosts;
	}


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
	 * Migrate an instance to one given host
	 * 
	 * @param instance
	 *            The id of instance to migrate
	 * @param host
	 *            The host to migrate the instance to
	 */
	public abstract void liveMigration(String instanceId, Host host)  throws HostFailureException, HostNotFoundException;

	/**
	 * Migrate an instance to one host chose by the engine
	 * 
	 * @param instance
	 *            The id of instance to migrate
	 *            
	 * @return The host to which the instance is migrating
	 */
	public abstract Host liveMigration(String instanceId)  throws HostFailureException, HostNotFoundException;

	
	/**
	 * Offload one given host of its VMs 
	 * 
	 * @param host
	 *            The host to offload
	 * @throws HostNotFoundException 
	 * @throws HostFailureException
	 */
	public abstract void offLoad(Host host)  throws HostNotFoundException, HostFailureException;
	
	
	/**
	 * Terminate this.
	 */
	public abstract void terminate();

}
