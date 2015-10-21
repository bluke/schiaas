package org.simgrid.schiaas.engine.storage;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.Storage;

/**
 * Interface for StorageEngine: internals to manage the storage virtualization
 * 
 * @author julien.gossa@unistra.fr
 */
public abstract class StorageEngine {
	
	/** The Storage of this */
	protected Storage storage;

	/**
	 * Enumerates the possible request to the storage. 
	 */
	public static enum REQUEST {
		PUT, GET, DELETE, LIST;
	}

	/**
	 * Unique constructor.
	 * 
	 * @param storage
	 *            The Storage of this.
	 */
	public StorageEngine(Storage storage) {
		this.storage = storage;
	}
		
	public abstract void doRequest(REQUEST get, Data data) throws TransferFailureException, HostFailureException, TimeoutException;
}
