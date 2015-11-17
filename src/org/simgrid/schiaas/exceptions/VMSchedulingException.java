package org.simgrid.schiaas.exceptions;

import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeScheduler;

/**
 * Exception for failure in VM scheduling, for instance when the plateform is full 
 * @author julien.gossa@unistra.fr
 *
 */
public class VMSchedulingException extends Exception{
	
	/** Scheduler throwing the error*/ 
	private final ComputeScheduler scheduler;
	
	/** Instance type of the VM */
	private final InstanceType instanceType;

	/** The reason because the VM was not scheduled */
	private final String reason;

	/**
	 * @param scheduler
	 * 			Scheduler in which the error happened
	 * @param instanceType
	 * 			Instance type for which the error happened
	 * @param reason
	 * 			The reason for which the error happened
	 */
	public VMSchedulingException(ComputeScheduler scheduler, InstanceType instanceType, String reason) {
		super("The scheduler "+scheduler.getClass() 
				+" did not schedule the instance of type "+instanceType.getId()
				+" because "+reason);
		
		this.scheduler = scheduler;
		this.instanceType = instanceType;
		this.reason = reason;
	}
	
	/**
	 * Get the scheduler
	 * @return this.scheduler
	 */
	public ComputeScheduler getScheduler(){
		return this.scheduler;
	}
	
	/**
	 * Get the instance type
	 * @return this.instanceType
	 */
	public InstanceType getInstanceType(){
		return this.instanceType;
	}

	/**
	 * Get the reason
	 * @return this.reason
	 */
	public String getReason(){
		return this.reason;
	}

	private static final long serialVersionUID = 1L;

}
