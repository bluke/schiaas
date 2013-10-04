package org.simgrid.schiaas.engine.rice;

import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.schiaas.Image;

/**
 * Used to track how many cores are currently used by VM on each compute's host.
 * @author julien
 */
public class RiceHost {
	
	/**
	 * Enumerates the possible status of image stored on the Host.
	 * @author julien
	 */
	protected static enum IMGSTATUS {
		TRANSFERING, AVAILABLE
	};
	
	protected Host host;
	protected int coreUsedByVMcount;
	protected double lastBootDate;
	protected Map<Image,IMGSTATUS> storedImages;
	
	protected RiceHost(Host host) {
		this.host = host;
		this.coreUsedByVMcount = 0;
		this.lastBootDate = -1e9;
		this.storedImages = new HashMap<Image,IMGSTATUS>();
	}

	protected String messageBox() {
		return "MB_RICE_"+host.getName();
	}
}
