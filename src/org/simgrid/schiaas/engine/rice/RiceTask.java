package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.Task;
import org.simgrid.schiaas.engine.ComputeEngine;
import org.simgrid.schiaas.engine.ComputeEngine.COMMAND;

/**
 * Rise command task.
 * @author julien.gossa@unistra.fr
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
