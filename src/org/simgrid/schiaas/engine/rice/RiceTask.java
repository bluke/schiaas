package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.engine.ComputeEngine;
import org.simgrid.schiaas.engine.ComputeEngine.COMMAND;
import org.simgrid.schiaas.engine.StorageEngine;
import org.simgrid.schiaas.engine.StorageEngine.REQUEST;

/**
 * Rise request task.
 * @author julien
 */
public class RiceTask extends Task{
	
	/** The command associated to this task. */
	protected ComputeEngine.COMMAND command;

	/** The data concerned by this task. */
	protected RiceInstance riceInstance;
	
	/** A default data size of a request. */
	protected static double commandDataSize = 1000;

	/** A default compute size of a request. */
	protected static double commandComputeSize = 0;
	
	/**
	 * Constructor of a RICE task.
	 * @param request the request type
	 * @param data the data concerned by the task
	 * @param response true if this task is a response
	 */
	protected RiceTask(RiceInstance riceInstance, COMMAND command) {
		super("RiceTask",commandComputeSize,commandDataSize);
	
		this.command = command;
		this.riceInstance = riceInstance;
	}
}
