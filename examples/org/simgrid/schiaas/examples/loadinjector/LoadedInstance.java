package org.simgrid.schiaas.examples.loadinjector;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.engine.compute.ComputeTools;

public class LoadedInstance {
	
	protected Compute compute;
	protected Instance instance;
	protected LoadedInstanceProcess loadedInstanceProcess;

	public LoadedInstance(Compute compute, String imageId, String instanceTypeId) {
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
