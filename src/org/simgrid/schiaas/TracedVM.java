package org.simgrid.schiaas;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.tools.Trace;

/**
 * This kind of VM only add the tracing system of SCHIaaS to SimGrid's VM
 * It supports only and all VM methods: please see its documentation for details.
 * @author julien.gossa@unistra.fr
 */
public class TracedVM extends VM {
	
	/** The trace of this TracedVM */
	Trace trace;

	/**
	 * Create a traced VM
	 * @param host the host of the VM
	 * @param name the name of the VM
	 * @param nCore the number of cores of the VM (deprecated?)
	 * @param ramSize the size of the RAM of the VM 
	 * @param migNetSpeed
	 * @param dpIntensity
	 */
	public TracedVM(Host host, String name, double nCore, int ramSize, int migNetSpeed, int dpIntensity) {
		super(host, name, ramSize, migNetSpeed, dpIntensity);
		
		trace = Trace.newCategorizedTrace("vm", name);
		
		trace.addProperty("n_cores", ""+nCore);
		trace.addProperty("ram_size", ""+ramSize);
		trace.addProperty("mig_net_speed", ""+migNetSpeed);
		trace.addProperty("dp_intensity", ""+dpIntensity);	
		
		trace.addEvent("state", "pending");
	}
	
	@Override
 	public void migrate(Host destination) throws HostFailureException {
 		trace.addEvent("state", "migrating");
 		super.migrate(destination);
 		trace.addEvent("state", "running");
 	}

	@Override
 	public void resume() {
 		trace.addEvent("state", "resuming");
 		super.resume();
 		trace.addEvent("state", "running");
 	}
	@Override
 	public void shutdown() {
 		trace.addEvent("state", "shutingdown");
 		super.shutdown();
  		trace.addEvent("state", "shutdown");
 	}

	@Override
 	public void start() {
 		trace.addEvent("state", "booting");
 		super.start();
 		trace.addEvent("state", "running");
 	}

	@Override
 	public void suspend() {
 		trace.addEvent("state", "suspending");
 		super.suspend();
 		trace.addEvent("state", "suspended");
 	}

	@Override
 	public void setBound(double bound) {
 		trace.addEvent("cpu_bound", ""+bound);
 		super.setBound(bound);
 	}
}
