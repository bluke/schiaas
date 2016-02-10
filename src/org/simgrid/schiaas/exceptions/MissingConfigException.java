package org.simgrid.schiaas.exceptions;

import org.simgrid.schiaas.Cloud;

/**
 * Exception for missing parameter in hash table parsed from config tag in cloud.xml 
 * @author lbertot@unistra.fr
 *
 */
public class MissingConfigException extends Exception{
		
	/** Name of the module throwing the error*/ 
	private final String module;
	
	/** ID of the missing parameter(s) */
	private final String missingPropID;
	
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
		
		this.module = cloud.getId()+"/"+type;
		this.missingPropID = propID;
	}
	
	/**
	 * 
	 * 
	 * @param module
	 * @param missingPropID
	 */
	public MissingConfigException(String module, String missingPropID) {
		super(module+" require the following configured properties: "+missingPropID);
	
		this.module = module;
		this.missingPropID = missingPropID;
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
		return this.missingPropID;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
