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
import org.simgrid.schiaas.tools.Trace;

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
	protected int freeCores;

	/** The current amount of RAM to be used by live VM. */
	protected int RAM;
	
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
	
	/** The trace of this Host */
	Trace trace;
	
	/**
	 * Constructor.
	 * @param rice The rice using this host.
	 * @param host The physical simgrid host.
	 * @param ramSize RAM available for VMs (in MB)
	 * @param diskSize disk available for VMs (in MB)
	 */
	protected RiceHost(Rice rice, Host host, int ramSize, int diskSize) {
		this.host = host;
		this.lastBootDate = -1e9;
		this.imagesCache = new HashMap<>();
		
		this.freeCores = (int) host.getCoreNumber();
		this.RAM = ramSize;
		this.freeRAM = ramSize;
		this.freeDisk = diskSize;
		
		this.instances = new Vector<>();
		
		bootMutex = new Mutex();
		
		availability = true;
		
		trace = rice.getCompute().getTrace().newCategorizedSubTrace("compute_host", host.getName());
		trace.addProperty("cores",""+freeCores);
		trace.addProperty("ram_size",""+ramSize);
		trace.addProperty("diskSize",""+diskSize);
		trace.addEvent("used_cores", ""+(host.getCoreNumber()-freeCores));
	}

	/**
	 * Constructor with default ram and disk values (4GB per core).
	 * @param rice The rice using this host.
	 * @param host The physical simgrid host.
	 */
	protected RiceHost(Rice rice, Host host) {
		this(rice, host, (int)host.getCoreNumber()*4096, (int)host.getCoreNumber()*4096);
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
		freeCores -= Double.parseDouble(riceInstance.getInstanceType().getProperty("core"));
		riceInstance.getTrace().addEvent("schedule", this.host.getName());
		trace.addEvent("used_cores", ""+(host.getCoreNumber()-freeCores));
	}

	/**
	 * Remove the given instance from the hosted ones
	 * Only manage the cores.
	 * @param riceInstance the instance to unhost
	 */
	public void removeInstance(RiceInstance riceInstance) {
		freeCores += Integer.parseInt(riceInstance.getInstanceType().getProperty("core"));
		this.instances.remove(riceInstance);
		trace.addEvent("used_cores", ""+(host.getCoreNumber()-freeCores));
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
		trace.addEvent("availability", ""+availability);
	}

	@Override
	public int getFreeCores() {
		return freeCores;
	}

	@Override
	public int getRam() {
		return RAM;
	}
	
	@Override
	public int getFreeRam() {
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
		return (int) (freeCores / Double.parseDouble(instanceType.getProperty("core")));
	}

	
	/**
	 * @return The id of the MSG message box used to send commands. 
	 */
	protected String messageBox() {
		return "MB_RICE_"+host.getName();
	}

	@Override
	public boolean isSLAViolated() {
		double totalSpeed = 0;
		int totalRAM = 0;
		for(Instance instance : instances) {
			totalSpeed += instance.SLA.speed;
			totalRAM += instance.SLA.RAM;
		}
		return (	totalSpeed <= host.getCoreNumber()*host.getSpeed() 
				&& 	totalRAM <= RAM);
	}


}
