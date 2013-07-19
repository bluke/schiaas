package org.simgrid.simiaas.api;

import org.simgrid.msg.Comm;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;
import org.simgrid.simiaas.api.ComputeControllerProcess.COMMAND;
import org.simgrid.simiaas.api.Instance.STATE;

/**
 * The hypervisor of the instance.
 * Used by the controller to command one instance.
 * Ran inside the VM of the instance.
 * Execute the tasks associated to the commands.
 * @author julien
 */
public class ComputeNodeProcess extends Process {
	
	/**
	 * The instance managed by this.
	 */
	protected Instance instance;
	
	
	/**
	 * The constructor.
	 * Starts the hypervisor on its hosting VM.
	 * @param instance The instance controlled by this.
	 * @throws HostNotFoundException Thrown when the hosting VM is not found.
	 */
	public ComputeNodeProcess(Instance instance) throws HostNotFoundException {
		super(instance, "Hypervisor_"+instance.getName());
		
		this.instance = instance;
		//this.mutex = new Mutex();
		
		this.start();
		//instance.suspend();
	}
	
	
	/**
	 * Infinite loop receiving commands through ControlTask, executing them, and sending a complete signal.
	 * Never exits. 
	 * @see org.simgrid.msg.Process#main(java.lang.String[])
	 */
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException, TaskCancelledException {
		ControlTask controlTask;
		
		Comm comm = Task.irecv(instance.getControlMessageBox());


		if (instance.imageTransferMutex != null) {
			Msg.verb(instance.getName()+" waiting image transfer for "+instance.getControlMessageBox());
			instance.imageTransferMutex.acquire();
			instance.imageTransferMutex.release();
		}
		
		do {	
						
			comm.waitCompletion();

			controlTask = (ControlTask) comm.getTask();
			
			Msg.verb(instance.getName()+" received command " +controlTask.command + "with delay " + controlTask.delay);

			waitFor(controlTask.delay);
			
			if (controlTask.command == ComputeControllerProcess.COMMAND.BOOT)
				instance.setState(Instance.STATE.BOOTING);
			
			try {
				controlTask.execute();
			} catch (TaskCancelledException e) {
			}
			controlTask.complete();
			
			Msg.verb(instance.getName()+" waiting for command in "+instance.getControlMessageBox());
			comm = Task.irecv(instance.getControlMessageBox());
			
		} while (controlTask.command != ComputeControllerProcess.COMMAND.SHUTDOWN);
	}
}
