package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.Host;

/**
 * Used to track how many cores are currently used by VM on each compute's host.
 * @author julien
 */
public class RiceHost {

	protected Host host;
	protected int coreUsedByVMcount;
	
	protected RiceHost(Host host) {
		this.host = host;
		coreUsedByVMcount = 0;
	}
}
