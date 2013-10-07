package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.engine.ComputeEngine.COMMAND;

/**
 * Useless for now.
 * @author julien
 */
public class RiceControllerProcess extends Process {
	
	protected Rice rice;
	protected RiceInstance riceInstance;
	protected COMMAND command;
	
	public RiceControllerProcess(Rice rice, COMMAND command, RiceInstance riceInstance) {
		super(rice.controller, rice.getCompute().getId()+" Rice Controller");
		this.rice = rice;
		this.riceInstance = riceInstance;
		this.command = command;
	}
	
	public void main(String[] args) throws MsgException {
		
		RiceTask riceTask = new RiceTask(riceInstance, command);
		
		riceTask.send(riceInstance.riceHost.messageBox());
	}
}
