package org.simgrid.schiaas;

import org.simgrid.msg.Host;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.tools.Trace;

//TODO : add dpIntensity / function of netBW for migration

/**
 * Represents an Instance, that is a VM controller by SimIaaS.
 * @author julien.gossa@unistra.fr
 */
public class Instance {
	
	/** The Compute of the instance */
	protected Compute compute;

	/** The VM of the instance */
	protected TracedVM vm;
	
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
		
	/** Tracing */
	protected Trace trace;
	
	/**
	 * Constructor to deploy and start a new instance.
	 * 
	 * @param compute The compute of the instance
	 * @param id The id of the instance
	 * @param image The image of the instance.
	 * @param instanceType The type of the instance.
	 * @param host 
	 */
	protected Instance(Compute compute, String id, Image image, InstanceType instanceType, Host host) {
		this.trace = compute.trace.newCategorizedSubTrace("instances", id);
		
		this.compute = compute;
		this.id = id;
		this.image = image;
		
		this.instanceType = instanceType;
		
		this.isPending = true;
		
		this.trace.addProperty("image", this.image.getId());
		this.trace.addProperty("instance_type", this.instanceType.getId());
		this.trace.addEvent("command", "runinstance");
	}

	/**
	 * @return The vm of this.
	 */
	public VM vm() {
		return this.vm;
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
	 * @return The trace of this.
	 */
	public Trace getTrace() {
		return this.trace;
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
	 * @return true if this instance is running.
	 */
	public boolean isRunning() {
		return (vm.isRunning() == 1 && !isTerminating);
	}

	/**
	 * Create the VM of this instance.
	 * Protected because it cannot be used directly by the user,
	 * only by the compute engine.
	 */
	protected void createVM() {
		this.vm = new TracedVM(compute.getComputeEngine().getComputeHostOf(this).getHost(), id+"_VM", 
				Double.parseDouble(instanceType.getProperty("core")),
				Integer.parseInt(instanceType.getProperty("ramSize")), 
				Integer.parseInt(instanceType.getProperty("netCap")),
				instanceType.getProperty("diskPath"), 
				Integer.parseInt(instanceType.getProperty("diskSize")),
				Integer.parseInt(instanceType.getProperty("migNetSpeed")),
				Integer.parseInt(instanceType.getProperty("dpIntensity")));
	}
	
	/**
	 * Start the vm of this instance.
	 * Protected because it cannot be used directly by the user,
	 * only by the compute engine.
	 */
	protected void start() {
		this.vm().start();
		isPending = false;
	}
	
	/**
	 * Suspend this instance.
	 */
	public void suspend() {
		this.compute.suspendInstance(this);
	}

	/**
	 * Resume this instance.
	 */
	public void resume() {
		this.compute.resumeInstance(this);
	}
		
	/**
	 * Reboot this instance.
	 */
	public void reboot() {
		this.compute.rebootInstance(this);
	}

	/**
	 * Terminate this instance.
	 */
	public void terminate() {
		this.compute.terminateInstance(this);
	}

	/**
	 * Of course.
	 * @return A string containing the name of the instance and its host.
	 */
	@Override
	public String toString() {
		return "Instance:" + id;
	}
	
}
