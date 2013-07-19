package org.simgrid.schiaas.api;


import java.util.HashMap;
import java.util.Vector;
import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Mutex;
import org.simgrid.schiaas.engine.simiaas.ComputeControllerProcess;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents a cloud in SimIaaS
 * @author julien.gossa@unistra.fr
 */
public class CloudTOBEMERGEWITHCOMPUTE
{
	protected static class VMHost {
		protected Host host;
		protected int index;
		protected int core;
		protected int maxInstancesCount;
		protected int instancesCount;
		protected int bootingInstancesCount;		
		protected int instanceID;
		
		/** The cache of the cloud: images and the Mutex used to wait for image transfer completion. */
		protected HashMap<Image, Mutex> imageCache;
				
		public VMHost(String name) throws HostNotFoundException {
			this.host = Host.getByName(name);
			this.instancesCount=0;
			this.bootingInstancesCount=0;
			this.instanceID=0;
			this.imageCache = new HashMap<Image, Mutex>();
		}
	}
	
	/** Represents the 3 options for cache: ON, OFF, and PRE(caching). */
	public static enum CACHE { ON, OFF, PRE };
	
	/** Name of the cloud */
	protected String name;
	
	/** Host of the controller of the cloud. */
	protected Host controller;
	
	/** Set of hosts of the cloud. */
	protected VMHost[] hosts;
	
	/** The cache option of the cloud. */
	protected CACHE caching;

	/** The power of a standard instance */
	public double standardPower = 10e9;
	
	/** The maximum amount of VM allowed per core */
	public int maxVMPerCore = 1;
	
	/** The maximum global amount of concurrent alive instances*/
	protected int maxInstancesCount;
	
	/** The current global amount of alive instances per host */
	protected int instancesCount;
	
	/** The process used to communicate with hypervisors. */
	protected ComputeControllerProcess computeControllerProcess;

	/** Maximum number of concurrent VM starting on one physical host. */
	protected int maxConcurrentBoots = 2;

	/** Delay between two boots. */
	protected int interBootDelay;
		
	/**
	 * Constructs a CloudTOBEMERGEWITHCOMPUTE from a XML DOM node
	 * @param node The XML DOM node
	 * @todo Retrieves the number of cores from cloud.xml, while it would be better from platform.xml
	 */
	public CloudTOBEMERGEWITHCOMPUTE(Node node) {
		
		Vector<VMHost> hostsVector = new Vector<VMHost>();
		
		try {
			NamedNodeMap attributes = node.getAttributes(); 
			this.name = attributes.getNamedItem("id").getNodeValue();
			this.controller = Host.getByName(attributes.getNamedItem("controller").getNodeValue());

			this.caching = CACHE.valueOf(attributes.getNamedItem("cache").getNodeValue());
			this.standardPower = Double.parseDouble(attributes.getNamedItem("standard_power").getNodeValue());
			this.maxVMPerCore = Integer.parseInt(attributes.getNamedItem("max_vm_per_core").getNodeValue());
			this.interBootDelay = Integer.parseInt(attributes.getNamedItem("inter_boot_delay").getNodeValue());
			
			this.maxInstancesCount = 0; 
			this.instancesCount = 0;
			
			NodeList hostList = node.getChildNodes();
			for (int i=0; i<hostList.getLength(); i++) {
				if (hostList.item(i).getNodeName().compareTo("host")==0) {
					VMHost vmhost = new VMHost(hostList.item(i).getAttributes().getNamedItem("id").getNodeValue());
					vmhost.maxInstancesCount =  maxVMPerCore * Integer.parseInt(hostList.item(i).getAttributes().getNamedItem("core").getNodeValue());
					this.maxInstancesCount+=vmhost.maxInstancesCount;
					hostsVector.add(vmhost);
				}
			}
			
			hosts = new VMHost[hostsVector.size()];
			hostsVector.toArray(hosts);
			
			this.computeControllerProcess = new ComputeControllerProcess(this);
			
		} catch (HostNotFoundException e) {
			Msg.critical("Host not found during the configuration of cloud "+name);
			e.printStackTrace();
		}
	}


	
	/**
	 * Check the availability of resources to run new instances, according to max_vm_per_core
	 * @return How many instances can still be ran on this cloud.
	 * @todo Add the flavor
	 */
	public int describeAvailability() {
		return maxInstancesCount-instancesCount;
	}

}

