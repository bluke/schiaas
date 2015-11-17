package org.simgrid.schiaas;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * Represents a VM image
 * 
 * @author julien.gossa@unistra.fr
 */
public class Image {

	/**
	 * A list of image properties.
	 */
	protected Map<String, String> properties;

	/**
	 * Unique constructor from a node of the XML config file
	 * 
	 * @param imageXMLNode
	 *            A node pointing out an image tag
	 * @author julien.gossa@unistra.fr
	 */
	public Image(Node imageXMLNode) {
		properties = new HashMap<>();

		for (int i = 0; i < imageXMLNode.getAttributes().getLength(); i++) {
			this.properties.put(imageXMLNode.getAttributes().item(i)
					.getNodeName(), imageXMLNode.getAttributes().item(i)
					.getNodeValue());
		}
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
	 * 
	 * @return the id of this
	 */
	public String getId() {
		return this.properties.get("id");
	}

	/**
	 * Of course
	 */
	@Override
	public String toString() {
		String res = getId() + ": ";
		for (Map.Entry<String, String> prop : this.properties.entrySet()) {
			res += prop.getKey() + "=" + prop.getValue() + " ";
		}
		return res;
	}

}
