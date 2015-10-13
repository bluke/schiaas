package org.simgrid.schiaas.engine.frise;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Task;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.StorageEngine;
import org.simgrid.schiaas.exceptions.MissingConfigException;

/**
 * Fast Reduced Implementation of Storage Engine.
 * Simple management of the storage virtualization.
 * Faster to simulate than RISE, but less accurate: storage does not wait for the reception of the request
 * to start the transfer the answer.
 * 
 * @author julien.gossa@unistra.fr
 */
public class FRise extends StorageEngine {
	
	/** Host of the controller of this. */
	protected Host controller;
	
	/** The data size of the request/answer without specific data. */
	protected static double commandDataSize = 1000;

	/** The compute size of a request/answer. */
	protected static double commandComputeSize = 0;

	
	public FRise(Storage storage) throws Exception {
		super(storage);
		
		try {
			storage.getConfig("controller");
		} catch (MissingConfigException e){
			Msg.critical(e.getMessage());
			throw e;
		}
		
		try {
			controller = Host.getByName(storage.getConfig("controller"));
		}catch (MissingConfigException e){
			Msg.critical(e.getMessage());
			e.printStackTrace();
		} catch (HostNotFoundException e) {
			Msg.critical("RISE controller host "+storage.getConfig("controller")+" not found.");
			e.printStackTrace();
		} catch (NullPointerException e) {
			Msg.critical("RISE controller host "+storage.getConfig("controller")+" not found.");
			e.printStackTrace();
		}
	}

	/**
	 * Getter of the storage of this 
	 */
	protected Storage getStorage() {
		return storage;
	}

	/**
	 * @return The message box used to put requests.
	 */
	protected String requestMessageBox() {
		return "MB_REQ_"+storage.getCloud().getId()+"_"+storage.getId();
	}

	
	/**
	 * Executes one request on this storage.
	 * @param request The request to execute.
	 * @param data The data concerned by the request.
	 * @throws TransferFailureException Whenever occurs at simgrid level.
	 * @throws HostFailureException Whenever occurs at simgrid level.
	 * @throws TimeoutException Whenever occurs at simgrid level.
	 */
	@Override
	public void doRequest(REQUEST request, Data data) 
			throws TransferFailureException, HostFailureException, TimeoutException {
		
		Task riseTask;
		Host hosts[] = {controller,Process.getCurrentProcess().getHost()};
		double flops[] = {commandComputeSize, commandComputeSize};
		double bytes[] = {}; 
		
		switch (request) {
		case GET: 
			double gbytes[] = {0, data.getSize(), commandDataSize, 0};			
			bytes = gbytes;
			
		case PUT:
			double pbytes[] = {0, commandDataSize, data.getSize(), 0};			
			bytes = pbytes;
			break;
			
		case DELETE:
		case LIST:
			double lbytes[] = {0, data.getSize(), data.getSize(), 0};			
			bytes = lbytes;
			break;
			
		}
		
		// Execute the storage task
		riseTask = new Task("RiseTask-"+request+"-"+data.getId(), hosts, flops, bytes);
		try {
			riseTask.execute();
		} catch (TaskCancelledException e) {
			Msg.critical("Something bad happened at the C level: "+e.getMessage());
			e.printStackTrace();
		}
	}	
}

