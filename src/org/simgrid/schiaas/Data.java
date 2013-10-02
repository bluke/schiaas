package org.simgrid.schiaas;

import org.simgrid.msg.Host;

/**
 * Represents an Instance, that is a VM controller by SimIaaS.
 * @author julien.gossa@unistra.fr
 */
public class Data {

	/** The ID of the data. */
	protected String id;
	
	/** The size of the data. */
	protected double size;
	
	/**
	 * Constructor of on data.
	 * 
	 * @param id
	 *            The id of this.
	 * @param size
	 *            The size of this.
	 */
	public Data(String id, double size) {
		this.id = id;
		this.size = size;
	}
	
	/** 
	 * Getter of id of this.
	 * @return the id of this.
	 */
	public String getId() {
		return id;
	}

	/** 
	 * Getter of the size of this.
	 * @return the size of this.
	 */
	public double getSize() {
		return size;
	}

	
}
