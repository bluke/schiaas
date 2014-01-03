package org.simgrid.schiaas;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * Represents an instance type, similar to Amazon's small, medium, large, ...
 * @author julien
 *
 */
public class InstanceType {

	/**
	 * A list of properties attached to this instance type
	 */
	protected Map<String, String> properties;
	
	/**
	 * Unique constructor from XML config file, setting default values.
	 * 
	 * @param instanceTypeXMLNode
	 *            A node pointing out one InstanceType tag in the XML config
	 *            file.
	 * @author julien.gossa@unistra.fr
	 */
	public InstanceType(Node instanceTypeXMLNode) {

		this.properties = new HashMap<String, String>();

		// default properties
		this.properties.put("core", "1");
		this.properties.put("ramSize", "256");
		this.properties.put("netCap", "10");
		this.properties.put("diskPath", "/default");
		this.properties.put("diskSize", "1000");
		this.properties.put("migNetSpeed", "10");
		this.properties.put("dpIntensity", "1");

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
	 */
	public String toString() {
		String res = this.properties.get("id") + ": ";
		for (Map.Entry<String, String> prop : this.properties.entrySet()) {
			res += prop.getKey() + "=" + prop.getValue() + " ";
		}
		return res;
	}
}
