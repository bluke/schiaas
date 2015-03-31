package org.simgrid.schiaas.engine.rice;

import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.Mutex;
import org.simgrid.schiaas.Image;

/**
 * An host for the RICE.
 * Used to track how many cores are currently used by VM on each compute's host.
 * @author julien.gossa@unistra.fr
 */
public class RiceHost {
	
	/**
	 * Enumerates the possible status of image stored on the Host.
	 */
	protected static enum IMGSTATUS {
		TRANSFERRING, AVAILABLE
	};
	
	/** The physical simgrid host. */
	protected Host host;
	
	/** The current amount of cores used by live VM. */
	protected int coreUsedByVMcount;
	
	/** The date of the last VM boot. */
	protected double lastBootDate;
	
	/** The set of instance images stored on this host. */
	protected Map<Image,IMGSTATUS> imagesCache;
	
	/** Mutex used to track the boot of local VMs. */
	protected Mutex bootMutex;
	
	/** The flag to know if this host is available to host new instances */
	protected boolean availability;
	
	/**
	 * Constructor.
	 * @param host The physical simgrid host.
	 */
	protected RiceHost(Host host) {
		this.host = host;
		this.coreUsedByVMcount = 0;
		this.lastBootDate = -1e9;
		this.imagesCache = new HashMap<Image,IMGSTATUS>();
		
		bootMutex = new Mutex();
		
		availability = true;
	}

	protected boolean isAvailable() {
		return availability;
	}
	
	/**
	 * @return The id of the MSG message box used to send commands. 
	 */
	protected String messageBox() {
		return "MB_RICE_"+host.getName();
	}
}
