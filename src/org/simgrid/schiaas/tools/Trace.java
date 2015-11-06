package org.simgrid.schiaas.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.simgrid.msg.Msg;

/**
 * Handle the tracing in a very flexible way.
 * There are the different informations:
 * - Entity: the element of the simulation, atomic or aggregated, that is traced 
 * 	 ex: Compute, Storage, Host, Instance
 * - Property: static information, with no date, linked to an entity
 *   ex: Host or instance name
 * - Event: information with date, type and value, linked to an entity 
 * 
 * @author julien.gossa@unistra.fr
 *
 */
public class Trace {
	public static Trace root = null;
	
    private static FileWriter fstream;
    private static BufferedWriter out;

	public static char fieldSep = '\t';
	public static char entitySep = ':';
	public static char replacement = '_';

	public static final String filename = "schiaas.trace";
	
	protected Trace parent;
	protected String category;
	protected String name;
	protected String fullname;

	public Trace(Trace parent, String category, String name) {
		this.parent = parent;
		this.name = name.replace(entitySep, replacement).replace(fieldSep, replacement);
		this.category = category.replace(entitySep, replacement).replace(fieldSep, replacement);

		if (parent == null) {
			this.fullname = this.name;
		} else {
			this.fullname = parent.fullname+entitySep+this.category+entitySep+this.name;
		}		
	}
	
	public Trace(Trace parent, String name) {
		this.parent = parent;
		this.name = name.replace(entitySep, replacement).replace(fieldSep, replacement);

		if (parent == null) {
			this.fullname = this.name;
		} else {
			this.fullname = parent.fullname+entitySep+this.name;
		}		
	}

	private String format(String value) {
		return value.replace(fieldSep, replacement);
	}
	
	public void addProperty(String prop, String value) {
		Trace.write(fullname+fieldSep+format(prop)+fieldSep+format(value));
	}

    public void addProperties(Map<String,String> prop) {
		for (Entry<String, String> e : prop.entrySet()) {
			this.addProperty(e.getKey(), e.getValue());
		}
    }

	public void addEvent(String type, String value) {
		Trace.write(fullname+entitySep+type+fieldSep+Msg.getClock()+fieldSep+format(value));		
	}
	
    public Trace newCategorizedSubTrace(String category, String name) {
    	return new Trace(this, category, name);
    }
	
	public Trace newSubTrace(String name) {
		return new Trace(this, name);
	}
	
    public static Trace newTrace(String name) {
    	if (root == null) init();
    	return new Trace(root, name);
    }

    public static Trace newCategorizedTrace(String category, String name) {
    	if (root == null) init();
    	return new Trace(root, category, name);
    }

    
    public static void init(String description) {
    	init();
    	root.addProperty("description", description);
    }
    
    public static void init() {
    	 try {
			fstream = new FileWriter(filename, false);
		} catch (IOException e) {
			Msg.critical("Error while opening the trace file "+filename);
			e.printStackTrace();
		}
    	out = new BufferedWriter(fstream);
    	
    	root = new Trace(null,"root");
    	root.addProperty("date", (new Date()).toString());
    }
    
	public static void close() {
		try {
			out.close();
			fstream.close();
		} catch (IOException e) {
			Msg.critical("Error while closing the trace file ");
			e.printStackTrace();
		}
		
	}

    public static void write(String logstr) {
    	try {
			out.write(logstr+'\n');
		} catch (IOException e) {
			Msg.critical("Writing in the trace file failed.");
			e.printStackTrace();
		}
    }
    
}
