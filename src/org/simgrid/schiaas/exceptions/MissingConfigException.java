package org.simgrid.schiaas.exceptions;

public class MissingConfigException extends Exception{
	
	private String cloud;
	private String module;
	private String param;
	
	public MissingConfigException(String cloud, String type, String propID) {
		super("In cloud "+cloud+", the "+type+"-engine configuration parameter "+propID+" was not found.");
		
		this.cloud = cloud;
		this.module = type;
		this.param = propID;
	}
	
	public String getCloud(){
		return this.cloud;
	}

	public String getModule(){
		return this.module;
	}
	
	public String getParameter(){
		return this.param;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
