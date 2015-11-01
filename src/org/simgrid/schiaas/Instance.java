package org.simgrid.schiaas;

import org.simgrid.msg.Host;
import org.simgrid.msg.Msg;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.tracing.Trace;

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
	 */
	protected Instance(Compute compute, String id, Image image, InstanceType instanceType, Host host) {
		this.vm = new TracedVM(	host, id+"_VM", 
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
		
		this.trace = compute.trace.newCategorizedSubTrace("instance", id);
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
	public int isRunning() {
		return vm.isRunning();
	}

	/**
	 * Suspend this instance.
	 */
	public void suspend() {
		this.trace.addEvent("command", "suspend");
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.SUSPEND,this);
	}

	/**
	 * Resume this instance.
	 */
	public void resume() {
		this.trace.addEvent("command", "resume");
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.RESUME,this);
	}
	
	/**
	 * Save this instance.
	 * Currently handled as suspend() and will be implemented whenever it is supported by simgrid.
	 */
	public void save() {
		this.trace.addEvent("command", "save");
		//TODO create save whenever it is supported by simgrid
		Msg.warn("Instance.save() is currently handled as Instance.suspend()");
		this.suspend();
	}

	/**
	 * Restore this instance.
	 * Currently handled as resume() and will be implemented whenever it is supported by simgrid.
	 */
	public void restore() {
		this.trace.addEvent("command", "restore");
		//TODO create restore whenever it is supported by simgrid
		Msg.warn("Instance.restore() is currently handled as Instance.resume()");
		this.resume();
	}
	
	/**
	 * Reboot this instance.
	 */
	public void reboot() {
		this.trace.addEvent("command", "reboot");
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.REBOOT,this);
	}

	/**
	 * Terminate this instance.
	 */
	public void terminate() {
		this.trace.addEvent("command", "terminate");
		this.isTerminating = true;
		this.compute.computeEngine.doCommand(ComputeEngine.COMMAND.SHUTDOWN,this);
	}

	/**
	 * of course.
	 * 
	 * @return A string containing the name of the instance and its host.
	 */
	public String toString() {
		return "Instance:" + id;
	}
	
}
