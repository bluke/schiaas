package org.simgrid.schiaas.loadinjector;

import java.util.Map;
import java.util.Random;

import org.simgrid.msg.HostFailureException;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.tools.Trace;

public abstract class AbstractInjector {

	protected String id;
	protected Cloud cloud;
	protected Map<String,String> config;
	
	protected static Map<String, AbstractInjector> abstractInjectors;
	
	protected Random random; 	
	protected Trace trace;
	
	public AbstractInjector(String id, Cloud cloud, Map<String,String> config) {
		this.id = id;
		this.cloud = cloud;
		this.config = config;
		
		long seed;
		try {
			seed = Long.parseLong(config.get("random_seed"));
		} catch(NumberFormatException e) {
			seed = System.currentTimeMillis();
		}
		this.random = new Random(seed);
		this.trace = Trace.newCategorizedTrace("injector",id);
		this.trace.addProperty("injector", ""+this.getClass());
		this.trace.addProperty("random_seed", ""+seed);
	}

	/** @return the id of this injector */
	public String getId() {
		return id;
	}
	
	/**
	 * Do the injection. Exits when the injection is finished.
	 * @throws HostFailureException 
	 */
	public abstract void run() throws HostFailureException;

}
