package org.simgrid.schiaas;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.engine.compute.ComputeEngine;

//TODO : add dpIntensity / function of netBW for migration

/**
 * Represents an Instance, that is a VM controller by SimIaaS.
 * @author julien.gossa@unistra.fr
 */
public class Instance extends org.simgrid.msg.VM {
	
	/** The Compute of the instance */
	protected Compute compute;
	
	/** The ID of the instance. */
	protected String id;
	
	/** The image of the instance. */
	protected Image image;

	/** The type of the instance. */
	protected InstanceType instanceType;
	
	/** True if the machine is pending boot */
	protected boolean isPending;

	/** True if the machine is terminating */
	protected boolean isTerminating;
		
	
	/**
	 * Constructor to deploy and start a new instance.
	 * 
	 * @param compute The compute of the instance
	 * @param id The id of the instance
	 * @param image The image of the instance.
	 * @param instanceType The type of the instance.
	 */
	protected Instance(Compute compute, String id, Image image, InstanceType instanceType, Host host) {
		super(	host, id, 
				Integer.parseInt(instanceType.getProperty("core")),
				Integer.parseInt(instanceType.getProperty("ramSize")), 
				Integer.parseInt(instanceType.getProperty("netCap")),
				instanceType.getProperty("diskPath"), 
				Integer.parseInt(instanceType.getProperty("diskSize")),
				Integer.parseInt(instanceType.getProperty("migNetSpeed")),
				Integer.parseInt(instanceType.getProperty("dpIntensity")));

		this.compute = compute;
		this.id = id;
		this.image = image;
		
		this.instanceType = instanceType;
		
		this.isPending = true;
	}

	/**
	 * @return The id of this.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @return The image of this.
	 */
	public Image getImage() {
		return this.image;
	}

	/**
	 * @return The id of the image of this.
	 */
	public String getImageId() {
		return this.image.getId();
	}

	/**
	 * @return The type of this.
	 */
	public InstanceType getInstanceType() {
		return this.instanceType;
	}

	/**
	 * @return The id of the type of this.
	 */
	public String instanceTypeId() {
		return this.instanceType.getId();
	}

	/**
	 * In addition to VM state.
	 * 
	 * @return true if this is pending (i.e. is waiting for start because of
	 *         image deployment or congestion management)
	 */
	public boolean isPending() {
		return this.isPending;
	}

	/**
	 * In addition to VM state.
	 * 
	 * @return true if this instance is terminating.
	 */
	public boolean isTerminating() {
		return this.isTerminating;
	}


	/**
	 * Suspend this instance.
	 */
	public void suspend() {
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.SUSPEND,this);
	}

	/**
	 * Resume this instance.
	 */
	public void resume() {
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.RESUME,this);
	}
	
	/**
	 * Save this instance.
	 * Currently handled as suspend() and will be implemented whenever it is supported by simgrid.
	 */
	public void save() {
		//TODO create save whenever it is supported by simgrid
		Msg.warn("Instance.save() is currently handled as Instance.suspend()");
		this.suspend();
	}

	/**
	 * Restore this instance.
	 * Currently handled as resume() and will be implemented whenever it is supported by simgrid.
	 */
	public void restore() {
		//TODO create restore whenever it is supported by simgrid
		Msg.warn("Instance.restore() is currently handled as Instance.resume()");
		this.resume();
	}
	
	/**
	 * Reboot this instance.
	 */
	public void reboot() {
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.REBOOT,this);
	}

	/**
	 * Create an error : Instances cannot be started, they must be asked by Compute.runInstance().
	 */
	public void start() {
		Msg.critical("Start command is prohibited on Instance: use Compute.runInstance() instead");
	}

	/**
	 * Terminate this instance.
	 */
	public void terminate() {
		this.isTerminating = true;
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.SHUTDOWN,this);
	}

	/**
	 * Terminate this instance.
	 * Handled as terminate() and create a warning.
	 */
	public void shutdown() {
		Msg.warn("Shutdown command on Instance are intercepted and handled as terminate");
		this.terminate();
	}

	
	/**
	 * of course.
	 * 
	 * @return A string containing the name of the instance and its host.
	 */
	public String toString() {
		return "Instance:" + getName();
	}
}
