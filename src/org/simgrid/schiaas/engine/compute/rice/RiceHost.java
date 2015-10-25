package org.simgrid.schiaas.engine.compute.rice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.Mutex;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeHost;

/**
 * Host at Compute level, with cloud admin-level information.
 * @author julien.gossa@unistra.fr
 */
public class RiceHost implements ComputeHost {
	
	/**
	 * Enumerates the possible status of image stored on the Host.
	 */
	protected static enum IMGSTATUS {
		TRANSFERRING, AVAILABLE
	};
	
	/** The underlying physical host. */
	public Host host;
		
	/** The current amount of free cores to be used by live VM. */
	protected double freeCores;

	/** The current amount of free RAM to be used by live VM. */
	protected int freeRAM;

	/** The current amount of free disk to be used by live VM. */
	protected int freeDisk;

	/** The instances hosted on this host */
	protected Vector<Instance> instances;
	
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
	 * @param ramSize RAM available for VMs (in MB)
	 * @param diskSize disk available for VMs (in MB)
	 */
	protected RiceHost(Host host, int ramSize, int diskSize) {
		this.host = host;
		this.lastBootDate = -1e9;
		this.imagesCache = new HashMap<Image,IMGSTATUS>();
		
		this.freeCores = host.getCoreNumber();
		this.freeRAM = ramSize;
		this.freeDisk = diskSize;
		
		this.instances = new Vector<Instance>();
		
		bootMutex = new Mutex();
		
		availability = true;
	}

	/**
	 * Constructor with default ram and disk values (4GB per core).
	 * @param host The physical simgrid host.
	 */
	protected RiceHost(Host host) {
		this(host, (int)host.getCoreNumber()*4096, (int)host.getCoreNumber()*4096);
	}

	
	@Override
	public List<Instance> getHostedInstances() {
		return instances;
	}

	/**
	 * Add the given instance to the hosted ones
	 * Only manage the cores.
	 * @param riceInstance the instance to host
	 */
	public void addInstance(RiceInstance riceInstance) {
		this.instances.add(riceInstance);
		freeCores -= riceInstance.getCoreNumber();
	}

	/**
	 * Remove the given instance from the hosted ones
	 * Only manage the cores.
	 * @param riceInstance the instance to unhost
	 */
	public void removeInstance(RiceInstance riceInstance) {
		this.instances.remove(riceInstance);
		freeCores += riceInstance.getCoreNumber();
	}

	
	@Override
	public Host getHost() {
		return host;
	}
	
	@Override
	public boolean isAvailable() {
		return availability;
	}

	@Override
	public void setAvailability(boolean availability) {
		this.availability = availability;
	}

	@Override
	public double getFreeCores() {
		return freeCores;
	}

	@Override
	public double getFreeRam() {
		return freeRAM;
	}

	@Override
	public double getFreeDisk() {
		return freeDisk;
	}

	/**
	 * Only check for core availability
	 */
	@Override
	public int canHost(InstanceType instanceType) {
		return (int) (freeCores / Integer.parseInt(instanceType.getProperty("core")));
	}

	
	/**
	 * @return The id of the MSG message box used to send commands. 
	 */
	protected String messageBox() {
		return "MB_RICE_"+host.getName();
	}


}
