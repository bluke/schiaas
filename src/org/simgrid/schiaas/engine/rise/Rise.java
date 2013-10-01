package org.simgrid.schiaas.engine.rise;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
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
	
	public Rise(Storage storage) {
		super(storage);
		
		try {
			controller = Host.getByName(storage.getProperty("controller"));
		} catch (HostNotFoundException | NullPointerException e) {
			Msg.critical("RISE controller host "+storage.getProperty("controller")+" not found.");
			e.printStackTrace();
		}
	}


	@Override
	public void doRequest(REQUEST get, Data data) {
		// TODO Auto-generated method stub
		
	}
	
}
