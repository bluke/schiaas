package org.simgrid.schiaas.loadinjector;

import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.tools.XMLConfigReader;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Injector {
	
	private static Map<String, AbstractInjector> injectors;
	
	public static void init(String cloudXMLFileName) {
        Msg.debug("Injectors initialization");
        
		injectors = new HashMap<String, AbstractInjector>();
        
        XMLConfigReader xmlConfigReader = new XMLConfigReader(cloudXMLFileName);
        NodeList nodeList = xmlConfigReader.getNodeList();
       
        for (int i=0; i<nodeList.getLength(); i++) {
        	if (nodeList.item(i).getNodeName().compareTo("injector") == 0) {
	        	AbstractInjector injector = newInjector(nodeList.item(i));
	        	getInjectors().put(injector.id, injector);
        	}
        }
	}
	
	/**
	 * Create a new injector.
	 * @param injectorXMLNode XML node describing the injector in the configuration file
	 */
	public static AbstractInjector newInjector(Node injectorXMLNode) {
		String id = injectorXMLNode.getAttributes().getNamedItem("id")
				.getNodeValue();
		String className = injectorXMLNode.getAttributes().getNamedItem("class")
				.getNodeValue();
		Cloud cloud = SchIaaS.getCloud(
				injectorXMLNode.getAttributes().getNamedItem("cloud")
				.getNodeValue());
		
		Map<String,String> config = null;
		NodeList nodes = injectorXMLNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName().compareTo("config") == 0) {
				config = XMLConfigReader.getConfig(nodes.item(i));
			}
		}
		
		AbstractInjector injector = null;
		
		try {
			// Creating and running the injector			
			injector = (AbstractInjector)Class.forName(className)
					.getConstructor(String.class, Cloud.class, Map.class).newInstance(id, cloud, config);
		} catch (Exception e) {
			Msg.critical("Something wrong happened while loading the injector "
					+ className + ": "+e.getMessage());
			e.printStackTrace();
		}
		
		return injector;
	}

	public static Map<String, AbstractInjector> getInjectors() {
		return injectors;
	}
}
