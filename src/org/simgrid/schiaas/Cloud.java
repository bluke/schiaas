package org.simgrid.schiaas;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.HostFailureException;

import org.simgrid.msg.Msg;
import org.simgrid.schiaas.tools.Trace;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class encapsulates all services provided by a particular instance of a
 * cloud.
 * 
 * @author julien.gossa@unistra.fr
 */
public class Cloud {

	/** ID of the cloud */
	protected String id;

	/** Computation as a service such as EC2 */
	protected Compute compute;

	/** Storages as a service such as S3, EBS */
	protected Map<String, Storage> storages;

	/** Tracing */
	protected Trace trace;
	
	
	/**
	 * Unique constructor from an XML node
	 * 
	 * @param cloudXMLNode
	 *            The XML node pointing at the cloud tag from the cloud XML
	 *            config file
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 */
	public Cloud(Node cloudXMLNode) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		this.storages = new HashMap<>();

		this.id = cloudXMLNode.getAttributes().getNamedItem("id")
				.getNodeValue();
		
		this.trace = Trace.newCategorizedTrace("cloud", id);

		Msg.debug("Cloud initialization: " + id);

		NodeList nodes = cloudXMLNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			
			if (nodes.item(i).getNodeName().compareTo("compute") == 0) {
				this.compute = new Compute(this, nodes.item(i));
			}

			if (nodes.item(i).getNodeName().compareTo("storage") == 0) {
				Storage storage = new Storage(this, nodes.item(i));
				this.storages.put(storage.getId(), storage);
			}
		}
	}

	/**
	 * 
	 * @return the id of the cloud
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * 
	 * @return the compute component of the cloud
	 */
	public Compute getCompute() {
		return this.compute;
	}

	/**
	 * 
	 * @return the collection of storage components of the cloud
	 */
	public Collection<Storage> getStorages() {
		return this.storages.values();
	}

	/**
	 * 
	 * @param storageId
	 *            the id of the storage component
	 * @return the storage component of the id
	 */
	public Storage getStorage(String storageId) {
		return this.storages.get(storageId);
	}
		
	/**
	 * Terminate the cloud, by terminating all of its components
	 * 
	 * @throws HostFailureException
	 */
	public void terminate() throws HostFailureException {
		Msg.verb("Terminating " + this);
		if (this.compute != null)
			this.compute.terminate();
		
		for (Storage storage : this.storages.values()) {
			storage.terminate();
		}
	}

	/**
	 * Of course
	 */
	public String toString() {
		return ("Cloud:" + id);
	}

}
