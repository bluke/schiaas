package org.simgrid.schiaas;

import org.w3c.dom.Node;

public class Network {
	
	protected String id;
	protected Cloud cloud;

	/**
	 * 
	 * @param cloud
	 * @param networkXMLNode
	 * @TODO got to do it, and no idea how.
	 */
	public Network(Cloud cloud, Node networkXMLNode){
		this.cloud = cloud; 
	}
	
	public String getId() {
		return id;
	}

	public void terminate() {
		// TODO Auto-generated method stub
		
	}
	
}
