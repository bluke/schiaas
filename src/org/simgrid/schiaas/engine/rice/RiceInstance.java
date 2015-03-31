package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.Host;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;

/**
 * An instance as manipulated in RICE.
 * Basically update the RiceHost and track the instance's dedicated state 
 * (i.e. states that are not at VM level).
 * @author julien.gossa@unistra.fr
 */
public class RiceInstance extends Instance {

	/** The physical host running this instance. */
	protected RiceHost riceHost;
	
	/**
	 * Constructor.
	 * @param id The id of this instance.
	 * @param image The image of this instance.
	 * @param instanceType The type of this instance.
	 * @param riceHost The physical host of this instance.
	 */
	protected RiceInstance(String id, Image image, InstanceType instanceType, RiceHost riceHost) {
		super(id, image, instanceType, riceHost.host);
		this.riceHost = riceHost;
		this.isPending = true;
	}
	
	/**
	 * @return The physical host of this.
	 */
	@Override
	protected Host getHost() {
		return this.riceHost.host;
	}
}
