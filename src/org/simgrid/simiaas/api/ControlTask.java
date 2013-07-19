package org.simgrid.simiaas.api;

import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.simiaas.api.ComputeControllerProcess.COMMAND;

/**
 * Task to control an instance.
 * @author julien
 */
public class ControlTask extends Task{
	
	/**
	 * The command associated to this task.
	 */
	protected ComputeControllerProcess.COMMAND command;
	
	/**
	 * The instance associated to this task.
	 */
	protected Instance instance;
	
	/**
	 * A default data size of a command.
	 */
	protected double commandDataSize = 1000;
	
	/**
	 * True if complete, false otherwise.
	 */
	protected boolean complete;
	
	/**
	 * Delay after communication, used to delay concurrent boots.
	 */
	protected double delay;
	
	/**
	 * A message box used for data transfer with the relevant contructor.
	 */
	protected String messageBox;
		
	/**
	 * Signaling the completion to the ControlProcess
	 */
	protected void complete() {
		complete=true;
		instance.cloud.computeControllerProcess.enqueueControlTask(this);
	}

	/**
	 * Constructs a task to trigger communications from the cloud controller to a given messagebox
	 * @param instance The instance targeted by the command of this.
	 * @param command The command of this.
	 */
	protected ControlTask(double dataSize, String messageBox) {
		super("TransferTask",0,dataSize);
		this.messageBox = messageBox;
		this.command = ComputeControllerProcess.COMMAND.TRANSFER;
	}
	
	/**
	 * Constructs a task to control a given instance.
	 * Sets the task fields (compute duration and data size) according to the command.
	 * @param instance The instance targeted by the command of this.
	 * @param command The command of this.
	 */
	protected ControlTask(Instance instance, ComputeControllerProcess.COMMAND command)
	{
		super();
		
		this.command=command;
		this.instance=instance;
		this.complete=false;
		
		this.delay=0;


		setName(command.toString()+"_Task");
		
		switch (command)
		{
		case BOOT :
			setComputeDuration(instance.image.getBootComputeDuration());
			setDataSize(commandDataSize);
			delay=instance.cloud.interBootDelay*(instance.vmhost.bootingInstancesCount-1);
			break;
			
		case REBOOT :
		 	setComputeDuration(instance.image.shutdownComputeDuration + instance.image.getBootComputeDuration());
			setDataSize(commandDataSize);
			break;
			
		case SUSPEND :
		 	setComputeDuration(instance.image.suspendComputeDuration);
			setDataSize(commandDataSize);
			break;	
			
		case RESUME :
		 	setComputeDuration(instance.image.resumeComputeDuration);
			setDataSize(commandDataSize);
			break;	
			
		case SHUTDOWN :
		 	setComputeDuration(instance.image.shutdownComputeDuration);
			setDataSize(commandDataSize);		
		}
	}
	

}
