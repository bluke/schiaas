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

	/** A counter to set instances' id. */
	protected int instancesId;

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
	 */
	public ComputeEngine(Compute compute, Collection<Host> hosts) {
		this.compute = compute;
		this.instancesId = 0;
	}

	/**
	 * 
	 * @return The compute of this.
	 */
	public Compute getCompute() {
		return this.compute;
	}

	public int getInstanceNumber() {
		return this.instancesId;
	}

	/**
	 * Run a new instance, using a simple round-robin scheduling
	 * 
	 * @param image
	 *            The image of the instance.
	 * @param instanceType
	 *            The type of the instance.
	 * @return The instance, about to be started
	 * @throws HostNotFoundException
	 *             when the physical host is not found.
	 */
	@Deprecated
	/**see Compute.runInstance()*/
	public Instance runInstance(Image image, InstanceType instanceType) {
		Instance instance = newInstance(this.compute.getCloud().getId() + "-"
				+ this.instancesId, image, instanceType);

		this.instancesId++;

		doCommand(ComputeEngine.COMMAND.START, instance);
		return instance;
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
	public abstract Instance newInstance(String id, Image image,
			InstanceType instanceType);

	/**
	 * 
	 * @param instanceType
	 *            One type of instance.
	 * @return The number of available instances of the given type.
	 */
	public abstract int describeAvailability(InstanceType instanceType);

	/**
	 * Execute a given command on a given instance.
	 * 
	 * @param command
	 *            The command to execute.
	 * @param instance
	 *            The target instance.
	 */
	public void doCommand(COMMAND command, Instance instance) {
		// TODO add here any logic that you want to be common to all commands
		// irrespective of the implementation
		switch (command) {
		case START:
			this.instancesId++;
			break;
		case SHUTDOWN:
			break;
		case SUSPEND:
			break;
		case RESUME:
			break;
		case REBOOT:
			break;
		}

		this.doCustomizedCommand(command, instance);
	}

	/**
	 * This method implements implementation specific command logic
	 * 
	 * @param command
	 *            The command to execute
	 * @param instance
	 *            The target instance
	 */
	protected abstract void doCustomizedCommand(COMMAND command,
			Instance instance);

	/**
	 * Terminate this.
	 */
	public abstract void terminate();

}
