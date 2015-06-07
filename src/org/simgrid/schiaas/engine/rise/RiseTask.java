package org.simgrid.schiaas.engine.rise;

import org.simgrid.msg.Task;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.engine.StorageEngine;
import org.simgrid.schiaas.engine.StorageEngine.REQUEST;

/**
 * Rise request task.
 * @author julien.gossa@unistra.fr
 */
public class RiseTask extends Task{
	
	/** The request associated to this task. */
	protected StorageEngine.REQUEST request;

	/** The data concerned by this task. */
	protected Data data;
	
	/** A default data size of a request. */
	protected static double requestDataSize = 1000;

	/** A default compute size of a request. */
	protected static double requestComputeSize = 0;
	
	/**
	 * Constructor of a RISE task.
	 * @param request the request type
	 * @param data the data concerned by the task
	 * @param response true if this task is a response
	 */
	protected RiseTask(REQUEST request, Data data, boolean response) {
		super("RiseTask",requestComputeSize,requestDataSize);
	
		this.request = request;
		this.data = data;
		
		switch (request) {
		case PUT:
			if (response == false) setBytesAmount(data.getSize());
		case GET: 
			if (response == true) setBytesAmount(data.getSize());
			break;
		case LIST:
		case DELETE:
			break;
		}

	}
	
}
