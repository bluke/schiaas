package org.simgrid.schiaas.engine;

import java.util.Collection;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;

/**
 * An engine of Compute.
 * 
 * @author julien
 */
public abstract class ComputeEngine {

	/** The compute of this */
	protected Compute compute;

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
	}

	/**
	 * 
	 * @return The compute of this.
	 */
	public Compute getCompute() {
		return this.compute;
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
	 * Terminate this.
	 */
	public abstract void terminate();

}
