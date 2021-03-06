package org.simgrid.schiaas.engine.storage.rise;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.storage.StorageEngine;
import org.simgrid.schiaas.exceptions.MissingConfigException;

/**
 * Reduced Implementation of Storage Engine.
 * Simple management of the storage virtualization.
 * 
 * @author julien.gossa@unistra.fr
 */
public class Rise extends StorageEngine {
	
	/** Host of the controller of this. */
	protected Host controller;
	
	public Rise(Storage storage) throws Exception {
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
		} catch (HostNotFoundException | NullPointerException e) {
			Msg.critical("RISE controller host "+storage.getConfig("controller")+" not found.");
			e.printStackTrace();
		}
	}

	/**
	 * Getter of the storage of this 
	 * @return The Storage of this engine
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
	 * @param riseTask The task subject of the response.
	 * @return The message box used to put responses.
	 */
	protected String responseMessageBox(RiseTask riseTask) {
		String dataString = "";
		if (riseTask.data != null)
			dataString = "_"+riseTask.data.getId();
		
		return "MB_RESP_"+storage.getCloud().getId()+"_"+storage.getId()+"_"
				+riseTask.request+dataString;
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
		
		@SuppressWarnings("unused")
		RiseProcess riseProcess = new RiseProcess(this, request, data);
		
		// Send the request
		RiseTask reqTask = new RiseTask(request, data, false);
		try {
			reqTask.send(requestMessageBox());
		} catch (MsgException e) {
			Msg.critical("Something bad happened at the C level: "+e.getMessage());
			e.printStackTrace();
		}

		switch (request) {
		case GET: 
		case LIST:
			@SuppressWarnings("unused")
			RiseTask respTask = (RiseTask) RiseTask.receive(responseMessageBox(reqTask));
			break;
		case PUT:
		case DELETE:
			break;
		}
	}	
}
