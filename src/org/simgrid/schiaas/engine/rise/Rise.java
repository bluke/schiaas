package org.simgrid.schiaas.engine.rise;

import java.util.Collection;
import java.util.HashSet;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Storage;
import org.simgrid.schiaas.engine.StorageEngine;

/**
 * Simple Cloud Storage Implementation
 * 
 * @author julien
 */
public class Rise extends StorageEngine {
	
	/** Host of the controller of this. */
	protected Host controller;
	
	protected Collection<String> completeTransfers;
	
	public Rise(Storage storage) {
		super(storage);
		
		try {
			controller = Host.getByName(storage.getConfig("controller"));
		} catch (HostNotFoundException e) {
			Msg.critical("RISE controller host "+storage.getConfig("controller")+" not found.");
			e.printStackTrace();
		} catch (NullPointerException e) {
			Msg.critical("RISE controller host "+storage.getConfig("controller")+" not found.");
			e.printStackTrace();
		}
		
		completeTransfers = new HashSet<String>();
	}

	/**
	 * Getter of the storage of this 
	 * @author julien
	 */
	protected Storage getStorage() {
		return storage;
	}

	/**
	 * 
	 * @return the message box used to put requests
	 */
	protected String requestMessageBox() {
		return "MB_REQ_"+storage.getCloud().getId()+"_"+storage.getId();
	}

	/**
	 * @param riseTask the task subject of the response
	 * @return the message box used to put responses
	 */
	protected String responseMessageBox(RiseTask riseTask) {
		String dataString = "";
		if (riseTask.data != null)
			dataString = "_"+riseTask.data.getId();
		
		return "MB_RESP_"+storage.getCloud().getId()+"_"+storage.getId()+"_"
				+riseTask.request+dataString;
	}

	
	@Override
	public void doRequest(REQUEST request, Data data) throws TransferFailureException, HostFailureException, TimeoutException {
		
		RiseProcess riseProcess = new RiseProcess(this, request, data);
		
		// Send the request
		RiseTask reqTask = new RiseTask(request, data, false);
		reqTask.send(requestMessageBox());

		switch (request) {
		case GET: 
		case LIST:
			RiseTask respTask = (RiseTask) RiseTask.receive(responseMessageBox(reqTask));
			break;
		case PUT:
			completeTransfers.add(data.getId());
			break;
		case DELETE:
			completeTransfers.remove(data.getId());
			break;
		}
	}
	
	/**
	 * @param dataId The ID of the data.
	 * @return true if the transfer of a data is complete.
	 */
	public boolean isTransferComplete(String dataId) {
		return completeTransfers.contains(dataId);
	}
	
}
