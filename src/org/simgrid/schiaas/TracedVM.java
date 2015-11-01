package org.simgrid.schiaas;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.VM;
import org.simgrid.schiaas.tracing.Trace;

public class TracedVM extends VM {
	
	Trace trace;

	public TracedVM(Host host, String name, int nCore, int ramSize, int netCap, String diskPath, int diskSize, int migNetSpeed, int dpIntensity) {
		super(host, name, nCore, ramSize, netCap, diskPath, diskSize, migNetSpeed, dpIntensity);
		
		trace = Trace.newCategorizedTrace("VirtualMachines", name);
		
		trace.addProperty("nCore", ""+nCore);
		trace.addProperty("ramSize", ""+ramSize);
		trace.addProperty("netCap", ""+netCap);
		trace.addProperty("diskPath", ""+diskPath);
		trace.addProperty("diskSize", ""+diskSize);
		trace.addProperty("migNetSpeed", ""+migNetSpeed);
		trace.addProperty("dpIntensity", ""+dpIntensity);	
	}
	
 	public void migrate(Host destination) throws HostFailureException {
 		trace.addEvent("state", "migrating");
 		super.migrate(destination);
 		trace.addEvent("state", "running");
 	}
 	
 	public void	restore() {
 		trace.addEvent("state", "restoring");
 		super.restore();
 		trace.addEvent("state", "running");
 	}

 	public void	resume() {
 		trace.addEvent("state", "resuming");
 		super.resume();
 		trace.addEvent("state", "running");
 	}

 	public void save() {
 		trace.addEvent("state", "saving");
 		super.save();
 		trace.addEvent("state", "running");
 	}

 	public void shutdown() {
 		trace.addEvent("state", "shutingdown");
 		super.shutdown();
 		trace.addEvent("state", "shutdown");
 	}

 	public void	start() {
 		trace.addEvent("state", "booting");
 		super.start();
 		trace.addEvent("state", "running");
 	}

 	public void	suspend() {
 		trace.addEvent("state", "suspending");
 		super.suspend();
 		trace.addEvent("state", "suspended");
 	}

 	public void setBound(int load) {
 		trace.addEvent("bound", ""+load);
 		super.setBound(load);
 	}

}
