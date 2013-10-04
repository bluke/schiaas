package org.simgrid.schiaas.engine;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Storage;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 19/07/13
 * Time: 15:43
 * To change this template use File | Settings | File Templates.
 */
public abstract class StorageEngine {
	
	/** The compute of this */
	protected Storage storage;

	/**
	 * Enumerates the possible request to the storage. 
	 * @author julien
	 */
	public static enum REQUEST {
		PUT, GET, DELETE, LIST;
	}

	/**
	 * Unique constructor.
	 * 
	 * @param compute
	 *            The compute of this.
	 */
	public StorageEngine(Storage storage) {
		this.storage = storage;
	}
		
	public abstract void doRequest(REQUEST get, Data data) throws TransferFailureException, HostFailureException, TimeoutException;

	public abstract boolean isTransferComplete(String dataId);	
}
