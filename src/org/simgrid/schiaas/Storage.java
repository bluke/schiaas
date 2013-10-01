package org.simgrid.schiaas;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.Msg;
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
	
	/** Contains all the properties of this storage instance */
	protected Map<String, String> properties;

	
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
		
		NodeList nodes = storageXMLNode.getChildNodes();
		
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName().compareTo("config") == 0) {
				NamedNodeMap configNNM = nodes.item(i).getAttributes();
				for (int j = 0; j < configNNM.getLength(); j++) {
					this.properties.put(configNNM.item(j).getNodeName(),
							configNNM.item(j).getNodeValue());
				}
			}
		}
		
		this.storedData = new HashMap<String, Data>();
		
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
	
	public String getId() {
		return this.id;
	}	
	
	/**
	 * for all properties
	 * 
	 * @param propId
	 *            the id of the property, as is the XML config file
	 * @return the property
	 */
	public String getProperty(String propId) {
		return this.properties.get(propId);
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
	 */
	public void put(Data data) {
		storedData.put(data.id, data);
		storageEngine.doRequest(StorageEngine.REQUEST.PUT, data);
	}
	
	/**
	 * Retrieve one data.
	 * @param data
	 * 			the data
	 */
	public void get(String dataId) {
		Data data = storedData.get(dataId);
		storageEngine.doRequest(StorageEngine.REQUEST.GET, data);
	}
	
	/**
	 * Delete one data.
	 * @param data
	 * 			the data
	 */
	public void delete(String dataId) {
		Data data = storedData.remove(dataId);
		storageEngine.doRequest(StorageEngine.REQUEST.DELETE, data);
	}
	
	/**
	 * List the stored data.
	 */
	public Collection<Data> list() {
		storageEngine.doRequest(StorageEngine.REQUEST.LIST, null);
		return storedData.values();
	}
	
}
