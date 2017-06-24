package org.simgrid.schiaas.loadinjector;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

/**
 * This class represents instances that have their CPU loaded.
 * It works only with instances that use only one core. 
 */
public class LoadedInstance {

	protected Compute compute;
	protected Instance instance;
	protected LoadedInstanceProcess loadedInstanceProcess;

	/**
	 * Loads the CPU of an exiting instance 
	 * @param compute the compute of the instance
	 * @param instance the instance to load
	 */
	public LoadedInstance(Compute compute, Instance instance) {
		this.compute = compute;
		this.instance = instance;		
		for (int i=0; i<1; i++) {
			this.loadedInstanceProcess = new LoadedInstanceProcess(this);

			try {
				Msg.info("DEBUG : WFRAS "+this);
				ComputeTools.waitForRunningAndStart(compute.getComputeEngine(), instance, this.loadedInstanceProcess);
				
			} catch (HostNotFoundException e) {
				Msg.critical("Something went wrong while trying to start the loading process "+this.loadedInstanceProcess.getName());
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Run an instance and load its CPU
	 * @param compute the compute managing this instance
	 * @param imageId the image of this instance
	 * @param instanceTypeId the type if this instance
	 * @throws VMSchedulingException whenever the instance can not be scheduled
	 */
	public LoadedInstance(Compute compute, String imageId, String instanceTypeId) throws VMSchedulingException {
		this(compute, compute.runInstance(imageId, instanceTypeId));
	}
	
	/**
	 * Run an instance and load its CPU
	 * @param compute the compute managing this instance
	 * @param imageId the image of this instance
	 * @param CPURequest the requested CPU (in core/second/second)
	 * @param RAMRequest the requested RAM
	 * @param diskRequest the requested disk
	 * @throws VMSchedulingException 
	 */
	public LoadedInstance(Compute compute, String imageId, double CPURequest, int RAMRequest, int diskRequest) throws VMSchedulingException {
		this(compute, compute.runInstance(
						compute.describeImage(imageId),
						new InstanceType(CPURequest, RAMRequest, diskRequest)));
	}

	/**
	 * 
	 * @return The instance that is loaded
	 */
	public Instance getInstance() {
		return this.instance;
	}
		
	/**
	 * Terminate this loaded instance
	 */
	public void terminate() {
		this.instance.terminate();
	}
	
	/**
	 * Set the CPU load of this instance
	 * @param load the load of the VM, ratio of the cloud standard_power
	 */
	public void setLoad(double load) {
		this.instance.vm().setBound(load * compute.getStandardPower()); 
		instance.getTrace().addEvent("cpu_load", ""+load);
	}

	
	/**
	 * The process to load this instance
	 * @author julien.gossa@unistra.fr
	 *
	 */
	private class LoadedInstanceProcess extends Process	{

		LoadedInstance loadedInstance;
		private static final double taskFlops = 1e100;
		
		public LoadedInstanceProcess(LoadedInstance loadedInstance) {
			super(instance.vm(), "LoadedInstanceProcess-"+instance.getId());
			this.loadedInstance = loadedInstance;
		}
		
		/**
		 * MSG's main: 
		 * execute a task in loop in order to burn the CPU
		 */
		@Override
		public void main(String[] args) throws MsgException {
			while(true) {
				Task task = new Task(
						"LoadedInstanceProcess-Task-"+loadedInstance.instance.getId(),
						taskFlops, 0 );
				task.execute();
			}
		}	
	}
}
