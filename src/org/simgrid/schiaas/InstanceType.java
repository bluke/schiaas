package org.simgrid.schiaas;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * Represents an instance type, similar to Amazon's small, medium, large, ...
 * @author julien.gossa@unistra.fr
 *
 */
public class InstanceType {

	/**
	 * A list of properties attached to this instance type
	 */
	protected Map<String, String> properties;

	/**
	 * Default constructor.
	 * 
	 * @author julien.gossa@unistra.fr
	 */
	public InstanceType() {
	
		this.properties = new HashMap<>();
		
		// default properties
		this.properties.put("id","anon");
		this.properties.put("core", "1");
		this.properties.put("ramSize", "256");
		this.properties.put("netCap", "125");
		this.properties.put("diskPath", "/default");
		this.properties.put("diskSize", "1000");
		this.properties.put("migNetSpeed", "125");
		this.properties.put("dpIntensity", "60");
	}

	/**
	 * Constructor with basic info.
	 * @param CPURequest The amount of core the instance needs (in core/second/second)
	 * @param RAMRequest The amount of RAM the instance needs (in MB)
	 * @param diskRequest The amount of disk the instance needs (in MB)
	 */
	public InstanceType(double CPURequest, int RAMRequest, int diskRequest) {
		this();
	
		this.properties.put("core", ""+CPURequest);
		this.properties.put("ramSize", ""+RAMRequest);
		this.properties.put("diskSize", ""+diskRequest);
	}

	
	
	/**
	 * Constructor from XML config file, setting default values.
	 * 
	 * @param instanceTypeXMLNode
	 *            A node pointing out one InstanceType tag in the XML config
	 *            file.
	 * @author julien.gossa@unistra.fr
	 */
	public InstanceType(Node instanceTypeXMLNode) {
		this();
		
		// properties from XML config file
		for (int i = 0; i < instanceTypeXMLNode.getAttributes().getLength(); i++) {
			this.properties.put(instanceTypeXMLNode.getAttributes().item(i)
					.getNodeName(), instanceTypeXMLNode.getAttributes().item(i)
					.getNodeValue());
		}
	}
	
	/**
	 * for all properties
	 * 
	 * @param propId
	 *            the id of the property, as is the XML config file or default
	 *            values.
	 * @return the property
	 */
	public String getProperty(String propId) {
		return this.properties.get(propId);
	}

	/**
	 * 
	 * @return The id of this.
	 */
	public String getId() {
		return this.properties.get("id");
	}

	/**
	 * Of course.
	 * @return Returns a string representation of this instance type. It 
	 * consists of the id followed by a colon. Properties are separated by a 
	 * space. Each properties are represented as a key followed by equal then 
	 * the value.
	 */
	@Override
	public String toString() {
		String res = this.properties.get("id") + ": ";
		for (Map.Entry<String, String> prop : this.properties.entrySet()) {
			res += prop.getKey() + "=" + prop.getValue() + " ";
		}
		return res;
	}
}
