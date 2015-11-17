package org.simgrid.schiaas.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.simgrid.msg.Msg;
import org.simgrid.schiaas.SchIaaS;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLConfigReader {
		
	private boolean isValid;
	
	NodeList nodeList;
	
	public XMLConfigReader(String xmlFilename) {
		this(xmlFilename, null);
	}
	
	public XMLConfigReader(String xmlFilename, String xsdFilename) {
		
		isValid = true;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setNamespaceAware(true);
		
		Validator validator = null;
		if (xsdFilename != null) {
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			Schema schema = null;
			try {
				schema = schemaFactory.newSchema(new StreamSource(SchIaaS.class.getResourceAsStream(xsdFilename)));
			} catch (SAXException e1) {
				Msg.critical("Error loading the XSD schema "+xsdFilename+" for "+xmlFilename+" : " + e1.getMessage());
				System.exit(0);
			}
			validator = schema.newValidator();
		}
		
		DocumentBuilder db;
		Document doc;
		
		try {
			db = dbf.newDocumentBuilder();						
			
			doc = db.parse(new File(xmlFilename));
			
			if (validator != null) {
				validator.setErrorHandler(new SimpleXMLErrorHandler());
				validator.validate(new DOMSource(doc));
			}
			
			this.nodeList = doc.getLastChild().getChildNodes();
			
		} catch (IOException e) {
			Msg.critical("Error while opening XML file "+xmlFilename);
			e.printStackTrace();
			System.exit(134);
		}  catch (Exception e) {
			Msg.critical("Error while reading the XML configuration file "+xmlFilename+": "+e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @return the list of nodes in the give XML file
	 */
	public NodeList getNodeList() {
		return this.nodeList;
	}

	public static Map<String,String> getConfig(Node node) {
		HashMap<String, String> config = new HashMap<>();
	
		NamedNodeMap configNNM = node.getAttributes();
		for (int j = 0; j < configNNM.getLength(); j++) {
			config.put(configNNM.item(j).getNodeName(),
							configNNM.item(j).getNodeValue());
		}
		
		return config;
	}
	
	/**
	 * Handles any errors that occured during the XML validation phase
	 * @author julien.gossa@unistra.fr
	 *
	 */
	final class SimpleXMLErrorHandler implements
			org.xml.sax.ErrorHandler {
		public void warning(SAXParseException e) throws SAXException {
			Msg.warn(e.getMessage());
		}

		public void error(SAXParseException e) throws SAXException {
			Msg.error(e.getMessage());
			isValid = false;
		}

		public void fatalError(SAXParseException e) throws SAXException {
			Msg.critical(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * 
	 * @return True if the given XML is valid according to the giver XSD
	 */
	public boolean isValid() {
		return this.isValid;
	}

}

