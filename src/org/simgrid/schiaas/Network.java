package org.simgrid.schiaas;


import org.w3c.dom.Node;

//TODO implement functionality and add XML structure in cloud file

public class Network {

	protected String id;
	protected Cloud cloud;

	protected double billCost;

	/**
	 * @return the total transfer cost induced by communication originating in
	 *         the cloud this network belongs to.
	 */
	public double getTransferCost() {
		return this.billCost;
	}

	/**
	 * 
	 * @param cloud
	 * @param networkXMLNode
	 * @TODO got to do it, and no idea how.
	 */
	public Network(Cloud cloud, Node networkXMLNode) {
		this.cloud = cloud;
	}

	public String getId() {
		return this.id;
	}

	public void terminate() {
		// TODO Auto-generated method stub
	}

}
