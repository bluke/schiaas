package org.simgrid.schiaas;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Cloud;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class is the equivalent of the MSG class and provides all services to
 * setup the different cloud platforms.
 * 
 * @author julien.gossa@unistra.fr
 */
public class SchIaaS {
	
	/** true if any XML validation errors have occurred during parsing. */
	public static boolean isXMLValid = true;
	
	/** Map of the clouds of the simulation */
	static protected Map<String, Cloud> clouds;

	/**
	 * Initialize the clouds from XML config file
	 * 
	 * @param cloudXMLFileName
	 *            The name of the XML config file (e.g. cloud.xml).
	 */
	public static void init (String cloudXMLFileName) {
        Msg.debug("SchIaaS initialization");      
        
        SchIaaS.clouds = new HashMap<String, Cloud>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setNamespaceAware(true);
		
		
		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema = null;
		try {
			schema = schemaFactory.newSchema(new StreamSource(SchIaaS.class.getResourceAsStream("/org/simgrid/schiaas/cloud.xsd")));
		} catch (SAXException e1) {
			Msg.critical("Error loading the XSD schema for the cloud XML file: " + e1.getMessage());
			System.exit(1);
		}
		
		DocumentBuilder db;
		Document doc;		
		
		 Validator validator = schema.newValidator();
		
		try {
			db = dbf.newDocumentBuilder();						
			
			doc = db.parse(new File(cloudXMLFileName));
			
			validator.setErrorHandler(new SimpleXMLErrorHandler());
			validator.validate(new DOMSource(doc));
			
			if (!SchIaaS.isXMLValid) {
				Msg.critical("Cloud xml configuration file is not valid");
				SchIaaS.terminate();
				System.exit(1);
			}
			
			
			NodeList nodes = doc.getLastChild().getChildNodes();
			for (int i=0; i<nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().compareTo("cloud") == 0) {
					String cloudid = nodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
					SchIaaS.clouds.put(cloudid,	new Cloud(nodes.item(i)));
				}
			}			
			
		} catch (IOException e) {
			Msg.critical("Cloud config file not found");
			e.printStackTrace();
			System.exit(134);
		}  catch (Exception e) {
			Msg.critical("Something wrong happened while initilizing SCHIaaS: "+e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} 

        
    }

	/**
	 * @return The map of all of the clouds.
	 */
	public static Map<String, Cloud> getClouds() {
		return SchIaaS.clouds;
	}

	
	/**
	 * @param cloudId
	 *            The ID of one cloud.
	 * @return The cloud of one ID.
	 */
	public static Cloud getCloud(String cloudId) {
		return SchIaaS.clouds.get(cloudId);
	}

	/**
	 * Terminate all of the clouds
	 * @throws HostFailureException
	 */
	public static void terminate() throws HostFailureException {
		Msg.verb("Terminating SchIaaS");
		for (Cloud cloud : SchIaaS.clouds.values()) {
			cloud.terminate();
		}
	}

	/**
	 * Handles any errors that occured during the XML validation phase
	 * @author mfrincu
	 *
	 */
	final static class SimpleXMLErrorHandler implements
			org.xml.sax.ErrorHandler {
		public void warning(SAXParseException e) throws SAXException {
			Msg.warn(e.getMessage());
		}

		public void error(SAXParseException e) throws SAXException {
			Msg.error(e.getMessage());
			SchIaaS.isXMLValid = false;
		}

		public void fatalError(SAXParseException e) throws SAXException {
			Msg.critical(e.getMessage());
			SchIaaS.isXMLValid = false;
			throw e;
		}
	}
}
