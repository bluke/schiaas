package org.simgrid.schiaas.engine.rice;

import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;

/**
 * An instance as manipulated in RICE.
 * Basically update the RiceHost and track the instance's dedicated state (i.e. states that are not at VM level).
 * @author julien
 */
public class RiceInstance extends Instance {

	protected RiceInstance(String id, Image image, InstanceType instanceType, RiceHost riceHost) {
		super(id, image, instanceType, riceHost.host);
		this.isPending = true;
		riceHost.coreUsedByVMcount+=Double.parseDouble(instanceType.getProperty("core"));
	}
	
	@Override
	public void start() {
		this.isPending = false;
		this.updateStartTime();
		super.start();
	}
	
	@Override
	public void shutdown() {
		this.updateShutdownTime();
		super.shutdown();
	}
	
}
