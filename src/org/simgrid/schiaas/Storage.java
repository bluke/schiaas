package org.simgrid.schiaas;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.billing.StorageBilling;
import org.simgrid.schiaas.engine.ComputeEngine;
import org.simgrid.schiaas.engine.StorageEngine;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//TODO implement functionality and add XML structure in cloud file

public class Storage {
	
	/** ID of this */
	protected String id;

	/** cloud using this */
	protected Cloud cloud;

	/** engine of this */
	protected StorageEngine storageEngine;
	
	/** Contains all the config of this storage instance */
	protected Map<String, String> config;

	
	/** collection of stored data */
	protected Map<String, Data> storedData;
	
	
	/** stores the policies for the billing of storage operations on this cloud */
	protected StorageBilling storageBilling = new StorageBilling();

	
	/**
	 * Unique constructor from XML config file.
	 * @param cloud
	 * @param storageXMLNode
	 *            TODO got to do it,, need time to develop the STOMAC - STOrage
	 *            MinimAl Component.
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Storage(Cloud cloud, Node storageXMLNode) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		this.cloud = cloud;
		this.id = storageXMLNode.getAttributes().getNamedItem("id").getNodeValue();
		String engine = storageXMLNode.getAttributes().getNamedItem("engine").getNodeValue();
		this.storedData = new HashMap<String, Data>();
		this.config = new HashMap<String, String>();
		
		NodeList nodes = storageXMLNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName().compareTo("config") == 0) {
				NamedNodeMap configNNM = nodes.item(i).getAttributes();
				for (int j = 0; j < configNNM.getLength(); j++) {
					
					this.config.put(
							configNNM.item(j).getNodeName(),
							configNNM.item(j).getNodeValue());
				}
			}
		}
		
		try {
			this.storageEngine = (StorageEngine) Class.forName(engine)
					.getConstructor(Storage.class)
					.newInstance(this);
		} catch (IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			Msg.critical("Something wrong happened while loading the storage engine "
					+ engine);
			e.printStackTrace();
		}
	}
	
	/**
	 * Getter
	 * @return the ID of this.
	 */
	public String getId() {
		return this.id;
	}	
	
	/**
	 * Getter
	 * @return the cloud of this
	 */
	public Cloud getCloud() {
		return this.cloud;
	}	
	
	
	/**
	 * Getter for all config properties.
	 * 
	 * @param propId
	 *            the id of the property, as is the XML config file
	 * @return the property
	 */
	public String getConfig(String propId) {
		return this.config.get(propId);
	}
	
	
	/**
      * @return a list of policies for the storage billing
      */
   public StorageBilling getStorageBillingPolicies() {
           return this.storageBilling;
   }

	
	public void terminate() {
		// TODO Auto-generated method stub
	}

	/**
	 * Store one data.
	 * Considers that the data is immediately stored before any actual transfer.
	 * @param data
	 * 			the data
	 * @throws TimeoutException 
	 * @throws HostFailureException 
	 * @throws TransferFailureException 
	 */
	public void put(Data data) throws TransferFailureException, HostFailureException, TimeoutException {
		storedData.put(data.id, data);
		storageEngine.doRequest(StorageEngine.REQUEST.PUT, data);
	}
	
	/**
	 * Instantaneously store one data (does not trigger data transfers)
	 * @param data
	 * 			the data
	 * @throws TimeoutException 
	 * @throws HostFailureException 
	 * @throws TransferFailureException 
	 */
	public void putInstantaneously(Data data) throws TransferFailureException, HostFailureException, TimeoutException {
		storedData.put(data.id, data);
	}
	
	/**
	 * Retrieve one data, whether it has been stored beforehand or not.
	 * If not, instantaneously store the data.
	 * @param data the data
	 * @return the very same data
	 * @throws TimeoutException 
	 * @throws HostFailureException 
	 * @throws TransferFailureException 
	 */
	public Data get(Data data) throws TransferFailureException, HostFailureException, TimeoutException {
		storedData.put(data.id, data);
		storageEngine.doRequest(StorageEngine.REQUEST.GET, data);
		return data;
	}

	/**
	 * Retrieve one data from its id.
	 * @param dataId the data id.
	 * @return the data;
	 * @throws TimeoutException 
	 * @throws HostFailureException 
	 * @throws TransferFailureException 
	 */
	public Data get(String dataId) throws TransferFailureException, HostFailureException, TimeoutException {
		Data data = storedData.get(dataId);
		storageEngine.doRequest(StorageEngine.REQUEST.GET, data);
		return data;
	}
	
	
	/**
	 * Delete one data.
	 * @param data
	 * 			the data
	 * @throws TimeoutException 
	 * @throws HostFailureException 
	 * @throws TransferFailureException 
	 */
	public void delete(String dataId) throws TransferFailureException, HostFailureException, TimeoutException {
		Data data = storedData.remove(dataId);
		storageEngine.doRequest(StorageEngine.REQUEST.DELETE, data);
	}
	
	/**
	 * List the stored data.
	 * @throws TimeoutException 
	 * @throws HostFailureException 
	 * @throws TransferFailureException 
	 */
	public Map<String, Data> list() throws TransferFailureException, HostFailureException, TimeoutException {
		storageEngine.doRequest(StorageEngine.REQUEST.LIST, null);
		return storedData;
	}

	/**
	 * Print the list the stored data.
	 */
	public void ls() {
		Msg.info("/"+cloud.getId()+"/"+id+"/$ ls");
		for(Data data : storedData.values()){
		    Msg.info(data.getId()+" "+data.getSize());
		}
	}
	
/**
 * Check whether the transfer of data is complete
 * @param dataId the ID of the data.
 * @return true if the transfer of the data is complete
 */
	public boolean isTransferComplete(String dataId) {
		return storageEngine.isTransferComplete(dataId);
	}
}
