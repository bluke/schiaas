package org.simgrid.schiaas.exceptions;

import org.simgrid.schiaas.Cloud;

/**
 * Exception for missing parameter in hash table parsed from config tag in cloud.xml 
 * @author lbertot@unistra.fr
 *
 */
public class MissingConfigException extends Exception{
	
	/** The cloud where the error happens */
	private final Cloud cloud;
	
	/** Name of the module throwing the error*/ 
	private final String module;
	
	/** name of the missing parameter */
	private final String param;
	
	/**
	 * 
	 * @param cloud
	 * 			The cloud in which the error happens
	 * @param type
	 * 			Engine type in which the error happened
	 * @param propID
	 * 			Name of the missing parameter
	 */
	public MissingConfigException(Cloud cloud, String type, String propID) {
		super("In cloud "+cloud.getId()+", the "+type+"-engine configuration parameter "+propID+" was not found.");
		
		this.cloud = cloud;
		this.module = type;
		this.param = propID;
	}
	
	/**
	 *  Get faulty cloud name
	 * @return this.cloud
	 */
	public Cloud getCloud(){
		return this.cloud;
	}

	/**
	 * Get name of faulty module
	 * @return this.module
	 */
	public String getModule(){
		return this.module;
	}
	
	/**
	 * Get missing parameter name
	 * @return this.param
	 */
	public String getParameter(){
		return this.param;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
