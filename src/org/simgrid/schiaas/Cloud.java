package org.simgrid.schiaas;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Network;
import org.simgrid.schiaas.Storage;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class encapsulates all services provided by a particular instance of a cloud.
 */
public class Cloud {

	/** id of the cloud */
	protected String id;

	/** Computation as a service such as EC2 */
	protected Compute compute ;
	
	/** Storages as a service such as S3, EBS */
	protected Map<String, Storage> storages;
	
	/** Network As a Service */
	protected Network network;  


    /** 
     * Unique constructor from an XML node 
     * @param cloudXMLNode The XML node pointing at the cloud tag from the cloud xml config file
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public Cloud(Node cloudXMLNode) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
    	
		storages = new HashMap<String, Storage>();
		
		id=cloudXMLNode.getAttributes().getNamedItem("id").getNodeValue();
		
		Msg.debug("Cloud initialization: "+id);
		
		NodeList nodes = cloudXMLNode.getChildNodes();
		for (int i=0; i<nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName().compareTo("compute") == 0) {
				compute = new Compute(this, nodes.item(i));
			}
			if (nodes.item(i).getNodeName().compareTo("storage") == 0) {
				String storageId = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
				storages.put(storageId, new Storage(this, nodes.item(i)));
			}
			if (nodes.item(i).getNodeName().compareTo("network") == 0) {
				network = new Network(this, nodes.item(i));
			}
		}
    }

    /**
     *  
     * @return the id of the cloud
     */
    public String getId() {
    	return id;
    }

    /**
     * 
     * @return the compute component of the cloud
     */
    public Compute getCompute() {
        return compute;
    }

    /**
     * 
     * @return the collection of storage components of the cloud
     */
    public Collection<Storage> getStorages() {
        return storages.values();
    }
    
    /**
     * 
     * @param storageId the id of the storage component 
     * @return the storage component of the id
     */
    public Storage getStorage(String storageId) {
    	return storages.get(storageId);
    }

    /**
     * 
     * @return the network component of the cloud
     */
    public Network getNetwork() {
        return network;
    }
    
    /**
     * Terminate the cloud, by terminating all of its components
     * @throws HostFailureException
     */
    public void terminate() throws HostFailureException {
    	Msg.verb("Terminating "+this);
    	if (compute != null )
    		compute.terminate();
    	
    	for (Storage storage : storages.values()) {
    		storage.terminate();
    	}
    	
    	if (network != null)
    		network.terminate();
    }
    
    /**
     * Of course
     */
    public String toString() {
    	return("Cloud "+id);
    }

}
