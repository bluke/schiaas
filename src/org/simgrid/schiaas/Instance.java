package org.simgrid.schiaas;

import org.simgrid.msg.Host;

//TODO : add dpIntensity / function of netBW for migration

/**
 * Represents an Instance, that is a VM controller by SimIaaS.
 * @author julien.gossa@unistra.fr
 */
public abstract class Instance extends org.simgrid.msg.VM {

	/**
	 * The last start and shutdown times of this instance
	 */
	protected long startTime, shutdownTime;
	
	/**
	 * The total billing time for running this instance.
	 * If for instance the instance has been saved to an external storage and
	 * later resurrected the time spent in the storage does not count. It will
	 * be billed separately.
	 */
	protected int billTime;
	
	/** Enumerates the different state of the instance. */
	// public static enum STATE { PENDING, BOOTING, RUNNING, REBOOTING, 
	// SUSPENDING, SUSPENDED, RESUMING, SHUTTINGDOWN, TERMINATED };

	/** The ID of the instance. */
	protected String id;
	
	/** The image of the instance. */
	protected Image image;

	/** The type of the instance. */
	protected InstanceType instanceType;

	//** The current state of the instance. */
	// protected STATE state;
	
	/** True if the machine is pending boot */
	protected boolean isPending;
	
	/** True if the machine is shutting down */
	protected boolean isTerminating;

	
	/**
	 * Constructor to deploy and start a new instance.
	 * 
	 * @param id
	 *            The id of the instance
	 * @param image
	 *            The image of the instance.
	 * @param instanceType
	 *            The type of the instance.
	 */
	protected Instance(String id, Image image, InstanceType instanceType, Host host) {
		super(host, id, Integer.parseInt(instanceType.getProperty("core")),
				Integer.parseInt(instanceType.getProperty("ramSize")), Integer
						.parseInt(instanceType.getProperty("netCap")),
				instanceType.getProperty("diskPath"), Integer
						.parseInt(instanceType.getProperty("diskSize")),
				Integer.parseInt(instanceType.getProperty("migNetSpeed")),
				Integer.parseInt(instanceType.getProperty("dpIntensity")));

		this.id = id;
		this.image = image;
		
		this.instanceType = instanceType;
		
		this.isPending = true;
		this.isTerminating = false;
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
	 * @return The physical host of this.
	 */
	protected abstract Host getHost();
	
	/** 
	 * @return The total billing time for running this instance
	 */
	public int getBillingTime() {
		return this.billTime;
	}

	// /**
	// * Sets the state of the instance.
	// * @param state the state of the instance, from now on.
	// */
	/*
	 * protected void setState(STATE state) { this.state=state;
	 * 
	 * //Trace.vmVariableSet(state.toString(),getName(),Msg.getClock());
	 * //Trace.hostPopState (this.getHost().getName(), "VM_STATE");
	 * //Trace.hostPushState (this.getHost().getName(), "VM_STATE",
	 * state.name());
	 * 
	 * Msg.verb("State of "+name+" changed to "+state); }
	 */

	// /**
	// * Gets the current state of the instance.
	// * @return the current state of the instance.
	// */
	// public STATE getState()
	// {
	// return state;
	// }

	/**
	 * In addition to VM state.
	 * 
	 * @return true if this is pending (i.e. is waiting for start because of
	 *         image deployment or congestion management)
	 */
	public boolean isPending() {
		return this.isPending;
	}

	public boolean isTerminating() {
		return this.isTerminating;
	}
	
	/**
	 * of course.
	 * 
	 * @return A string containing the name of the instance and its host.
	 */
	public String toString() {
		return "Instance:" + getName() + " (" + this.getHost().getName()+")";
	}

}
