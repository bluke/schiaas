package org.simgrid.schiaas.loadinjector;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

public class LoadedInstance {
	
	protected Compute compute;
	protected Instance instance;
	protected LoadedInstanceProcess loadedInstanceProcess;

	/**
	 * This instance has a CPU load
	 * @param compute the compute managing this instance
	 * @param imageId the image of this instance
	 * @param instanceTypeId the type if this instance
	 * @throws VMSchedulingException whenever the instance can not be scheduled
	 */
	public LoadedInstance(Compute compute, String imageId, String instanceTypeId) throws VMSchedulingException {
		this.compute = compute;
		this.instance = compute.runInstance(imageId, instanceTypeId);
		
		this.loadedInstanceProcess = new LoadedInstanceProcess(this);
		
		try {
			ComputeTools.waitForRunningAndStart(compute.getComputeEngine(), instance, this.loadedInstanceProcess);
		} catch (HostNotFoundException e) {
			Msg.critical("Something went wrong while trying to start the loading process "+this.loadedInstanceProcess.getName());
			e.printStackTrace();
		}
	}
	
	public void terminate() {
		this.instance.terminate();
	}
	
	public void setLoad(double load) {
		instance.vm().setBound((int)load);
		instance.getTrace().addEvent("load", ""+load);
	}

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
