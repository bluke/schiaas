package org.simgrid.schiaas.engine.rice;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.ComputeEngine;
import org.simgrid.schiaas.exceptions.MissingConfigException;

/**
 * Reduced Implementation of Compute Engine.
 * Simple management of the instances lifecycle.
 * 
 * @author julien.gossa@unistra.fr
 */
public class Rice extends ComputeEngine {

	/** Host of the controller of this. */
	protected Host controller;

	/** All the host of this. */
	protected Vector<RiceHost> riceHosts;
	
	/** The controller of migrations */
	protected MigrationController migrationController;

	/**
	 * Enumerates the possible off-load types.
	 */
	protected static enum OFFLOADTYPE {
		SEQUENTIAL, PARALLEL
	};

	/** the off-load type */
	protected OFFLOADTYPE offLoadType;
	
	
	/** Storage used for the images. */
	protected Storage imgStorage;

	/**
	 * Enumerates the possible image caching strategies.
	 */
	protected static enum IMGCACHING {
		ON, OFF, PRE
	};

	/** the caching strategy */
	protected IMGCACHING imgCaching;

	/** the delay between two consecutive boots */
	protected double interBootDelay;
	
	/** the index for the round robin to assign vm to pm */
	private int iAssignVM;
	
	/**
	 * Constructor
	 * 
	 * @param compute The compute of this. 	 
	 * @param hosts The set of physical hosts usable by this. 
	 * @throws MissingConfigException Thrown whenever one required configuration parameter is not found.
	 * @throws HostNotFoundException Thrown whenever one given host is not found. 
	 * @throws FileNotFoundException 
	 */
	public Rice(Compute compute, Collection<Host> hosts) throws MissingConfigException, HostNotFoundException, FileNotFoundException {
		super(compute, hosts);
		
		try{
			compute.getConfig("controller");
			compute.getConfig("image_storage");
			compute.getConfig("image_caching");
			compute.getConfig("inter_boot_delay");
		} catch (MissingConfigException e)
		{
			Msg.critical(e.getMessage());
			throw e;
		}

		// retrieving the controller
		try {
			this.controller = Host.getByName((String) compute.getConfig("controller"));
		} catch (HostNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e){
		
		}

		// retrieving the hosts
		this.riceHosts = new Vector<RiceHost>();
		for (Host host : hosts) {
			this.riceHosts.add(new RiceHost(host));
		}
		
		// Running the migrationController
		try {
			this.migrationController = new MigrationController(this, compute.getConfig("offloads_file"));
		} catch (MissingConfigException e)	{
		} 
		try {
			offLoadType = OFFLOADTYPE.valueOf(compute.getConfig("offload_type"));
		} catch (MissingConfigException e)	{
			offLoadType = OFFLOADTYPE.SEQUENTIAL;
		} 
		
		
		// retrieving the images
		Msg.info("storing images");
		imgStorage = compute.getCloud().getStorage(compute.getConfig("image_storage"));
		for (Image image : compute.describeImages().values()) {
			Data imgData = new Data("RICEIMG-"+image.getId(), Double.parseDouble(image.getProperty("size")));
			try {
				imgStorage.putInstantaneously(imgData);
			} catch (Exception e) {
				Msg.critical("Something bad happens when trying to store the images of RICE");
				e.printStackTrace();
			}
		}

		imgCaching = IMGCACHING.valueOf(compute.getConfig("image_caching"));
		interBootDelay = Double.parseDouble(getCompute().getConfig("inter_boot_delay"));
		
		// Init the VM to PM Assignation
		iAssignVM = 0;
	}

	/**
	 * Select one host to run a new instance of a given type. Basically search
	 * for one host having enough available cores to satisfy the type of
	 * instance.
	 * 
	 * @param instanceType
	 *            The type of instance.
	 * @return One host able to run an instance of this type. TODO (or not)
	 *         Generalize this by an abstraction to be able too plug any
	 *         strategy.
	 */
	public RiceHost assignVM(InstanceType instanceType) {
		
		if (compute.describeAvailability(instanceType.getId())==0)
			return null;
				
		double core = Double.parseDouble(instanceType.getProperty("core"));
		while (true) {
			RiceHost riceHost = this.riceHosts.get(iAssignVM);
			
			Msg.verb("Assign: probing PM " + iAssignVM + "/" + this.riceHosts.size() + " : asked cores=" + core
					+ ", used cores=" + riceHost.coreUsedByVMcount + "/"
					+ riceHost.host.getCoreNumber());
			
			iAssignVM = (iAssignVM+1)%this.riceHosts.size();

			if (riceHost.isAvailable() && (riceHost.host.getCoreNumber() - riceHost.coreUsedByVMcount) >= core)
				return riceHost;
		}
	}

	/**
	 * Checks the amount of instances of one given type that can be ran on this Compute.
	 * @param instanceType A type of instance
	 * @return The amount of instances of this type that can be ran
	 */
	public int describeAvailability(InstanceType instanceType) {
		double core = Double.parseDouble(instanceType.getProperty("core"));
		int availability = 0;
		for (int i = 0; i < this.riceHosts.size(); i++)
			availability += (this.riceHosts.get(i).host.getCoreNumber() - this.riceHosts.get(i).coreUsedByVMcount) / core;

		return availability;
	}

	/**
	 * Executes one command.
	 * @param command The command to execute.
	 * @param instance The instance concerned by the command.
	 */
	public void doCommand(COMMAND command, Instance instance) {
		RiceInstance riceInstance = (RiceInstance) instance;

		RiceControllerProcess rcp = new RiceControllerProcess(this, command, riceInstance);
		RiceNodeProcess rnp = new RiceNodeProcess(this, riceInstance.riceHost);
		
		try {
			rcp.start();
			rnp.start();
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened in RISE while trying to execute a command");
			e.printStackTrace();
		}
	}

	protected RiceHost getRiceHostOf(Host host) throws HostNotFoundException {
		for(RiceHost riceHost : riceHosts) {
			if (riceHost.host == host)
				return riceHost;
		}

		throw new HostNotFoundException("RiceHost not found for "+host);
	}	
		
	
	/**
	 * Migrate an instance to one given host
	 * 
	 * @param instance The instance to migrate
	 * @param host The host to migrate the instance to
	 * @throws HostFailureException, hostNotFoundException 
	 */
	@Override
	public void liveMigration(String instanceId, Host host) throws HostFailureException, HostNotFoundException {
		RiceInstance riceInstance = (RiceInstance) compute.describeInstance(instanceId);
		
		RiceHost fromHost = getRiceHostOf(riceInstance.riceHost.host);
		RiceHost toHost = getRiceHostOf(host);
		
		
		fromHost.coreUsedByVMcount -= riceInstance.getCoreNumber();
		toHost.coreUsedByVMcount += riceInstance.getCoreNumber();
		
		riceInstance.riceHost = toHost;
		
		riceInstance.migrate(toHost.host);
	}

	/**
	 * Migrate an instance to one host chosen by RICE
	 * 
	 * @param instance The instance to migrate
	 * @param host The host to migrate the instance to
	 * @throws HostFailureException, hostNotFoundException 
	 */
	@Override
	public Host liveMigration(String instanceId) throws HostFailureException, HostNotFoundException {
		RiceInstance riceInstance = (RiceInstance) compute.describeInstance(instanceId);
		RiceHost riceHost = assignVM(riceInstance.instanceType());
		if (riceHost == null) return null;
		
		liveMigration(instanceId, riceHost.host);
		
		return riceHost.host;
	}

	/**
	 * Process to handle one live migration, used to parallelize migrations during off-loads 
	 * @author julien.gossa@unistra.fr
	 */
	protected class LiveMigrationProcess extends Process {
		private String instanceId;	
		
		protected LiveMigrationProcess(String instanceId) throws HostNotFoundException {
			super(controller, "LiveMigrationProcess:"+instanceId);
			this.instanceId = instanceId;
			try {
				this.start();
			} catch(HostNotFoundException e) {
				Msg.critical("Something bad happend in the LiveMigrationProcess of RICE"+e.getMessage());
			}
		}

		public void main(String[] arg0) throws MsgException {
			liveMigration(instanceId);
		}
	}

	
	/**
	 * Offload one given host of its VMs 
	 * 
	 * @param host
	 *            The host to offload
	 * @throws HostNotFoundException 
	 * @throws HostFailureException
	 */
	@Override
	public void offLoad(Host host)  throws HostNotFoundException, HostFailureException {
		RiceHost riceHost = getRiceHostOf(host);
		
		riceHost.availability = false;
		
		for (Instance instance: compute.describeInstances()) {
			RiceInstance riceInstance = (RiceInstance) instance;
			if (riceInstance.riceHost.host == host)
				switch(offLoadType) {
				case SEQUENTIAL :
					liveMigration(instance.getId());
					break;
				case PARALLEL :
					new LiveMigrationProcess(instance.getId());
					break;
				default :
					Msg.critical("Off-load type not reconized");
				}
				
		}
	}
	
	/**
	 * Terminate this.
	 */
	@Override
	public void terminate() {
	}

	/**
	 * Selects one physical host and creates a new instance.
	 * @return The newly created instance, or null if no suitable physical host can be found.
	 */
	@Override
	public Instance newInstance(String id, Image image, InstanceType instanceType) {
		RiceHost riceHost = assignVM(instanceType);

		if (riceHost == null) return null;
		return new RiceInstance(id, image, instanceType, riceHost);
	}
		
}
