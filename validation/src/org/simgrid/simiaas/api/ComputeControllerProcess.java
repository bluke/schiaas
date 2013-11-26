package org.simgrid.simiaas.api;

import java.util.LinkedList;
import org.simgrid.msg.Comm;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;

/**
 * ControlProcesses are used by Controller to send commands to hypervisors.
 * @author julien
 */
public class ComputeControllerProcess extends Process {
	
	/**
	 * Enumerates the possible commands to control instances.
	 * @author julien
	 */
	public static enum COMMAND { TRANSFER, BOOT, SHUTDOWN, SUSPEND, RESUME, REBOOT, FINALIZE, ACQUIREMUTEX };
	
	/**
	 * The cloud using this control process.
	 */
	protected Cloud cloud;
	
	/**
	 * The cloud using this control process.
	 */
	protected LinkedList<ControlTask> queue;
	
	/**
	 * List of pending comms.
	 */
	protected LinkedList<Comm> pendingComms;
		

	
	/**
	 * Constructor for an immediate execution of the command.
	 * @param cloud The cloud using this control process.
	 * @throws HostNotFoundException Thrown when the controller of the cloud is not found.
	 */
	public ComputeControllerProcess(Cloud cloud) {
		super(cloud.controller, "ControlProcess:"+cloud.name);
		
		this.cloud=cloud;
		
		this.queue = new LinkedList<ControlTask>();
		
		this.pendingComms = new LinkedList<Comm>();
		
		try {
			this.start();
			//this.run();
		} catch(HostNotFoundException e) {
			Msg.critical("Something bad happend in SimIaaS"+e.getMessage());
		}
		
	}
	
	void enqueueControlTask(ControlTask ctask) {
		queue.add(ctask);
		if (queue.size()==1) {
			this.resume();
		}
		
	}
	
	/**
	 * Wait for controlProcessToWait, then send the command to the hypervisor oh the instance, 
	 * then wait for the completion signal from the hypervisor, finally exits. 
	 * @see org.simgrid.msg.Process#main(java.lang.String[])
	 */
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {

		ControlTask ctask;
		
		do {
			while (queue.isEmpty()) {
				suspend();
			}
			ctask = queue.poll();
			
			switch(ctask.command) {
			case TRANSFER :
				Msg.verb("Data transfer to "+ctask.messageBox);
				ctask.isend(ctask.messageBox);
				break;
			case ACQUIREMUTEX :
				ctask.instance.runningMutex.acquire();
				break;
			case FINALIZE : 
				break;

			default :
				Msg.verb("Processing command "+ctask.command+" regarding "+ctask.instance.getName());
				
				if ( !ctask.complete ) {
					ctask.instance.commandInit(ctask.command);

					ctask.isend(ctask.instance.getControlMessageBox());
					
					//pendingComms.add(ctask.isend(ctask.instance.getControlMessageBox()));
					Msg.verb("Command "+ctask.command+" send to "+ctask.instance.getControlMessageBox());
				} else {
					ctask.instance.commandFinalize(ctask.command);
					Msg.verb("Command "+ctask.command+" finalized");
				}
			}
		} while (ctask.command!=COMMAND.FINALIZE);
	}


}
