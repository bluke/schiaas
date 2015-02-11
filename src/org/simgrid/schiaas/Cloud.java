package org.simgrid.schiaas;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.HostFailureException;

import org.simgrid.msg.Msg;
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

	/** Network As a Service */
	protected Network network;

	
	/**
	 * Unique constructor from an XML node
	 * 
	 * @param cloudXMLNode
	 *            The XML node pointing at the cloud tag from the cloud XML
	 *            config file
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public Cloud(Node cloudXMLNode) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {

		this.storages = new HashMap<String, Storage>();

		this.id = cloudXMLNode.getAttributes().getNamedItem("id")
				.getNodeValue();

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
			
			if (nodes.item(i).getNodeName().compareTo("network") == 0) {
				this.network = new Network(this, nodes.item(i));
			}
			
			
			// billing section
			// TODO integrate into the rest
			/*
			if (nodes.item(i).getNodeName().compareTo("billing") == 0) {
				NodeList cnodes = nodes.item(i).getChildNodes();
				for (int j = 0; j < cnodes.getLength(); j++) {
					NodeList unodes = cnodes.item(j).getChildNodes();
					if (cnodes.item(j).getNodeName().compareTo("compute") == 0) {
						for (int k = 0; k < unodes.getLength(); k++) {
							if (unodes.item(k).getNodeName().compareTo("unit") == 0) {
								NamedNodeMap unitAttr = unodes.item(k)
										.getAttributes();

								this.compute.instanceTypes
										.get(unitAttr.getNamedItem("ref")
												.getNodeValue())
										.setBillingInfo(
												new ComputeBilling(
														Double.parseDouble(unitAttr
																.getNamedItem(
																		"fixed_price")
																.getNodeValue()),
														Integer.parseInt(unitAttr
																.getNamedItem(
																		"fixed_btu")
																.getNodeValue()),
														unitAttr.getNamedItem(
																"dynamic_price_file")
																.getNodeValue()));

							}
						}
					}
					if (cnodes.item(j).getNodeName().compareTo("network") == 0) {						
						for (int k = 0; k < unodes.getLength(); k++) {
							if (unodes.item(k).getNodeName().compareTo("unit") == 0) {
								NamedNodeMap unitAttr = unodes.item(k)
										.getAttributes();
								// TODO maybe the unit attribute could act as an
								// ID and netBills could be a Map
								this.netBilling.addNetworkBillingPolicy(
										unitAttr.getNamedItem("unit")
												.getNodeValue(),
										unitAttr.getNamedItem(
												"outgoing_price_file")
												.getNodeValue(),
										unitAttr.getNamedItem(
												"incoming_price_file")
												.getNodeValue());								
							}
						}
					}
					
					//TODO add storage billing and link it to storage (the ref in storage_billing to id in storage)
					if (cnodes.item(j).getNodeName().compareTo("storage") == 0) {
						for (int k = 0; k < unodes.getLength(); k++) {
							if (unodes.item(k).getNodeName().compareTo("unit") == 0) {
								NamedNodeMap unitAttr = unodes.item(k)
										.getAttributes();
								// TODO maybe the unit attribute could act as an
								// ID and netBills could be a Map
								this.storages.get(unitAttr.getNamedItem("ref")
										.getNodeValue()).getStorageBillingPolicies().addStorageBillingPolicy(
										unitAttr.getNamedItem("unit")
												.getNodeValue(),
										unitAttr.getNamedItem(
												"outgoing_price_file")
												.getNodeValue(),
										unitAttr.getNamedItem(
												"incoming_price_file")
												.getNodeValue(),
										unitAttr.getNamedItem(
												"storage_price_file")
												.getNodeValue());	
							}
						}						
					}					
				}
			}*/
		}
		//System.out.println(this.netBilling.getPolicy(NetworkBilling.NETWORK_UNIT.MEGABIT).getOutgoingPrice(10300));
		//System.out.println(this.storages.get("s3").getStorageBillingPolicies().getPolicy(StorageBilling.STORAGE_UNIT.GIGABYTE).getIncomingPrice(2000));
		
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
	 * 
	 * @return the network component of the cloud
	 */
	public Network getNetwork() {
		return this.network;
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

		if (this.network != null)
			this.network.terminate();

		// compute billing
	}

	/**
	 * Of course
	 */
	public String toString() {
		return ("Cloud " + id);
	}

}
