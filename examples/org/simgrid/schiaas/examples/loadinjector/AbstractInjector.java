package org.simgrid.schiaas.examples.loadinjector;

import java.util.Random;

import org.simgrid.msg.HostFailureException;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.tracing.Trace;

public abstract class AbstractInjector {

	protected Cloud cloud;
	protected String[] args;
	
	protected Random random; 	
	protected Trace trace;

	
	/**
	 * Constructor.
	 * @param cloud The cloud to inject
	 * @param args An array containing the arguments of the injector (First item is the name of the injector)
	 */
	public AbstractInjector(Cloud cloud, String[] args) {
		this.cloud = cloud;
		this.args = args;
		
		long seed = System.currentTimeMillis();
		this.random = new Random(seed);
		this.trace = Trace.newTrace("injector");
		this.trace.addProperty("injector", ""+this.getClass());
		this.trace.addProperty("random_seed", ""+seed);
	}
	
	/**
	 * Do the injection. Exit only when the injection is finished.
	 * @throws HostFailureException 
	 */
	public abstract void run() throws HostFailureException;
}
