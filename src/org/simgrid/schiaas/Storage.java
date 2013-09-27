package org.simgrid.schiaas;

import org.w3c.dom.Node;

public class Storage {

	protected String id;
	protected Cloud cloud;

	/**
	 * 
	 * @param cloud
	 * @param storageXMLNode
	 * TODO got to do it,, need time to develop the STOMAC - STOrage MinimAl Component.
	 */
	public Storage(Cloud cloud, Node storageXMLNode){
		this.cloud = cloud;
	}
	
	public String getId() {
		return id;
	}

	public void terminate() {
		// TODO Auto-generated method stub
		
	}

}
