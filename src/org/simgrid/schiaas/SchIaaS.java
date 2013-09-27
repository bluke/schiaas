package org.simgrid.schiaas;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Cloud;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This class is the equivalent of the MSG class and provides all services to setup the different cloud platforms.
 */
public class SchIaaS {

	/** Map of the clouds of the simulation */
    static protected Map<String, Cloud> clouds;

    /**
     * Initialize the clouds from XML config file
     * @param CloudXMLFileName The name of the XML config file (e.g. cloud.xml).
     */
    public static void init (String CloudXMLFileName){
        Msg.debug("SchIaaS initialization");
        
    	clouds = new HashMap<String, Cloud>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc;
		
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(new File(CloudXMLFileName));

			NodeList nodes = doc.getLastChild().getChildNodes();
			for (int i=0; i<nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().compareTo("cloud") == 0) {
					String cloudid = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
					clouds.put(cloudid,	new Cloud(nodes.item(i)));
				}
			}			
			
		} catch (IOException e) {
			Msg.critical("Cloud config file not found");
			e.printStackTrace();
			System.exit(0);
		}  catch (Exception e) {
			System.exit(0);
			e.printStackTrace();
		} 

        
    }

    /**
     * 
     * @param cloudId The id of one cloud.
     * @return The cloud of one id.
     */
    public static Cloud getCloud(String cloudId){
        return clouds.get(cloudId);
    }
    
    /**
     * 
     * @return All of the clouds.
     */
    public static Collection<Cloud> getClouds(){
       return clouds.values();
    }

    /**
     * Terminate all of the clouds
     * @throws HostFailureException
     */
	public static void terminate() throws HostFailureException {
		Msg.verb("Terminating SchIaaS");
		for (Cloud cloud : clouds.values()) {
			cloud.terminate();
		}
		
	}
}
