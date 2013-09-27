package org.simgrid.schiaas;

import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.Msg;
import org.w3c.dom.Node;

public class InstanceType {

	protected Map<String,String> properties;
	
	/**
	 * Unique constructor from XML config file, setting default values.
	 * @param instanceTypeXMLNode A node pointing out one InstanceType tag in the XML config file.
	 * @author julien.gossa@unistra.fr
	 */		
	public InstanceType(Node instanceTypeXMLNode) {

		properties = new HashMap<String,String>();

		// default properties
		properties.put("core", "1");  
		properties.put("ramSize", "256"); 
		properties.put("netCap", "10");		
		properties.put("diskPath", "/default"); 
		properties.put("diskSize", "1000");
		properties.put("migNetSpeed", "10");
		properties.put("dpIntensity", "1");

		// properties from xml config file
		for (int i=0; i<instanceTypeXMLNode.getAttributes().getLength(); i++) {
			Msg.info(instanceTypeXMLNode.getAttributes().item(i).getNodeName()+","+ instanceTypeXMLNode.getAttributes().item(i).getNodeValue() );
			properties.put(instanceTypeXMLNode.getAttributes().item(i).getNodeName(), instanceTypeXMLNode.getAttributes().item(i).getNodeValue());
		}		
	}	
	
	/**
	 * for all properties
	 * @param propId the id of the property, as is the XML config file or default values.
	 * @return the property
	 */
	public String getProperty(String propId) {
		return properties.get(propId);
	}
	
	/**
	 * 
	 * @return The id of this.
	 */
	public String getId() {
		return properties.get("id");
	}

	/**
	 * Of course.
	 */
	public String toString() {
		String res = properties.get("id") +": ";
		for (Map.Entry<String, String> prop : properties.entrySet()) {
			res+=prop.getKey() + "=" + prop.getValue()+" ";
		}
		return res;
	}
}

