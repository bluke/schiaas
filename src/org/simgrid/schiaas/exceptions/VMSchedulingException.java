package org.simgrid.schiaas.exceptions;

import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.InstanceType;

/**
 * Exception for failure in VM scheduling, for instance when the plateform is full 
 * @author julien.gossa@unistra.fr
 *
 */
public class VMSchedulingException extends Exception{
	
	/** The cloud where the error happens */
	private Cloud cloud;
	
	/** Name of the scheduler throwing the error*/ 
	private String scheduler;
	
	/** instance type of the VM */
	private InstanceType instanceType;
	
	/**
	 * 
	 * @param cloud
	 * 			ID of the cloud in which the error happens
	 * @param scheduler
	 * 			Scheduler in which the error happened
	 * @param instanceType
	 * 			Instance type for which the error happened
	 */
	public VMSchedulingException(Cloud cloud, String scheduler, InstanceType instanceType) {
		super("In cloud "+cloud.getId()+", the scheduler "+scheduler+" did not schedule the instance of type "+instanceType);
		
		this.cloud = cloud;
		this.scheduler = scheduler;
		this.instanceType = instanceType;
	}
	
	/**
	 *  Get faulty cloud name
	 * @return this.cloud
	 */
	public Cloud getCloud(){
		return this.cloud;
	}

	/**
	 * Get name of the scheduler
	 * @return this.scheduler
	 */
	public String getScheduler(){
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
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
