package org.simgrid.schiaas.process;
import org.simgrid.msg.Host;
import org.simgrid.msg.Process;

/**
 * Basic SchIaaS process. Every process managed by SchIaaS should use it
 * @author mfrincu
 *
 */
public abstract class SchIaaSProcess extends Process {

	private static int processId = 0;
	
	protected String instanceId = null;
	
	public String getAttachedInstance() {
		return this.instanceId;
	}
	
	public SchIaaSProcess(Host host, String instanceId, String[]args) {		
		super(host, "process_" + (++SchIaaSProcess.processId) + "_" + instanceId, args);
		this.instanceId = "process_" + (++SchIaaSProcess.processId) + "_" + instanceId;
	}	
}
