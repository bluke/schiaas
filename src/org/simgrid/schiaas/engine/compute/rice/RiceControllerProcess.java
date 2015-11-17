package org.simgrid.schiaas.engine.compute.rice;

import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.engine.compute.ComputeEngine.COMMAND;

/**
 * Process handling Compute commands from the controller side.
 * @author julien.gossa@unistra.fr
 */
public class RiceControllerProcess extends Process {
	
	protected Rice rice;
	protected RiceInstance riceInstance;
	protected COMMAND command;
	
	/**
	 * Constructor.
	 * @param rice The RICE concerned by the command.
	 * @param command The command to be executed.
	 * @param riceInstance The instance concerned by the command.
	 */
	public RiceControllerProcess(Rice rice, COMMAND command, RiceInstance riceInstance) {
		super(rice.controller, rice.getCompute().getId()+"-Rice-Controller-"+command+"-"+riceInstance.getId());
		this.rice = rice;
		this.riceInstance = riceInstance;
		this.command = command;
	}
	
	/**
	 * MSG's main: simply sends the task corresponding to the command 
	 * from the controller to the concerned node.
	 */
	@Override
	public void main(String[] args) throws MsgException {
		
		RiceTask riceTask = new RiceTask(riceInstance, command);
		
		riceTask.send(riceInstance.riceHost.messageBox());
	}
}
