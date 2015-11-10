package org.simgrid.schiaas.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.simgrid.msg.Msg;

/**
 * Handle the tracing in a very flexible way, using different informations:<br/>
 * - Entity: the element of the simulation, atomic or aggregated, that is traced<br/> 
 * 	 ex: Compute, Storage, Host, Instance<br/>
 * - Property: static information, with no date, linked to an entity<br/>
 *   ex: Host or instance name<br/>
 * - Event: information with date, type and value, linked to an entity<br/> 
 * <br/>
 * Produce the <b>schiaas.trace</b> file, in which each line is of the same format:<br/>
 * <i>entity key value</i><br/>
 * <i>key</i> being either the date for event, or the name for properties.<br/>
 * <br/>
 * This trace can be exploited using the script <b>trace-util.py</b><br/>
 * 
 * @author julien.gossa@unistra.fr
 *
 */
public class Trace {
	public static Trace root = null;
	
    private static FileWriter fstream;
    private static BufferedWriter out;

    /** Separate the fields in the trace file */
	public static char fieldSep = '\t';
	/** Separate the entity in the first field of the trace */ 
	public static char entitySep = ':';
	/** Replacement for reserved characters */
	public static char replacement = '_';

	/** The file to store the trace */
	public static final String filename = "schiaas.trace";
	
	/** The parent trace */
	protected Trace parent;
	/** The category of this trace */
	protected String category;
	/** The name of this trace */
	protected String name;
	/** The fullname of this trace, that is its name concatenated with parent's fullname */
	protected String fullname;

	/**
	 * Constructor.
	 * @param parent The parent of this trace.
	 * @param category The category of this trace.
	 * @param name The name of this trace.
	 */
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
	
	/**
	 * Constructor
	 * @param parent The trace to which this is a sub-trace.
	 * @param name The name of the trace.
	 */
	public Trace(Trace parent, String name) {
		this.parent = parent;
		this.name = name.replace(entitySep, replacement).replace(fieldSep, replacement);

		if (parent == null) {
			this.fullname = this.name;
		} else {
			this.fullname = parent.fullname+entitySep+this.name;
		}		
	}

	/**
	 * Format a string to comply to Trace format
	 * @param value The string to format.
	 * @return The formated string.
	 */
	private String format(String value) {
		return value.replace(fieldSep, replacement);
	}
	
	/**
	 * Add a property to the current trace, that is a non-time-related information.
	 * @param prop The name of the property.
	 * @param value The value of the property.
	 */
	public void addProperty(String prop, String value) {
		Trace.write(fullname+fieldSep+format(prop)+fieldSep+format(value));
	}

	/**
	 * Add a set of properties.
	 * @param prop The set of properties to add.
	 */
    public void addProperties(Map<String,String> prop) {
		for (Entry<String, String> e : prop.entrySet()) {
			this.addProperty(e.getKey(), e.getValue());
		}
    }

    /**
     * Add an event to the trace, that is a time-stamped value.
     * @param type The type of the event.
     * @param value The value of the event.
     */
	public void addEvent(String type, String value) {
		Trace.write(fullname+entitySep+type+fieldSep+Msg.getClock()+fieldSep+format(value));		
	}
	
	/**
	 * Create a categorized sub-trace to this trace. 
	 * @param category The category of the sub-trace.
	 * @param name The name of the sub-trace.
	 * @return The new sub-trace.
	 */
    public Trace newCategorizedSubTrace(String category, String name) {
    	return new Trace(this, category, name);
    }
	
    /**
     * Create a sub-trace to this trace.
     * @param name The name of the sub-trace.
     * @return The new sub-trace.
     */
	public Trace newSubTrace(String name) {
		return new Trace(this, name);
	}
	
	/**
	 * Create a new trace attached to the root trace.
	 * @param name The name of this trace.
	 * @return The new trace.
	 */
    public static Trace newTrace(String name) {
    	if (root == null) init();
    	return new Trace(root, name);
    }

    /**
     * Create a new categorized trace attached to the root trace.
     * @param category The category of the trace.
     * @param name The name of the trace.
     * @return The new categorized trace.
     */
    public static Trace newCategorizedTrace(String category, String name) {
    	if (root == null) init();
    	return new Trace(root, category, name);
    }

    /**
     * Init the trace.
     * @param description A description of the simulation.
     */
    public static void init(String description) {
    	init();
    	root.addProperty("description", description);
    }

    /**
     * Init the trace.
     */
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

	/**
	 * Write into the trace
	 * @param logstr the string to write. 
	 */
    private static void write(String logstr) {
    	try {
			out.write(logstr+'\n');
		} catch (IOException e) {
			Msg.critical("Writing in the trace file failed.");
			e.printStackTrace();
		}
    }
}
