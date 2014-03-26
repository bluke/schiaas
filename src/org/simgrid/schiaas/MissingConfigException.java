package org.simgrid.schiaas;

public class MissingConfigException extends Exception{
	
	private String cloud;
	private String type;
	private String param;
	
	public MissingConfigException(String cloud, String type, String propID) {
		super("In cloud "+cloud+", the "+type+"-engine configuration parameter "+propID+" was not found.");
		
		this.cloud = cloud;
		this.type = type;
		this.param = propID;
	}
	
	public String getCloud(){
		return this.cloud;
	}

	public String getType(){
		return this.type;
	}
	
	public String getParameter(){
		return this.param;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
