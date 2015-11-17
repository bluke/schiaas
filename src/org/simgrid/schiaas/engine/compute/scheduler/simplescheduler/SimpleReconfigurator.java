package org.simgrid.schiaas.engine.compute.scheduler.simplescheduler;

import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeHost;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

public class SimpleReconfigurator extends SimpleScheduler {
	
	int delay;
	boolean terminating;

	public SimpleReconfigurator(ComputeEngine computeEngine, Map<String, String> config) {
		super(computeEngine, config);
		
		this.delay = Integer.parseInt(config.get("delay"));
		this.terminating = false;
		
		SimpleReconfiguratorProcess p;
		try {
			 p = new SimpleReconfiguratorProcess(
					Host.getByName(config.get("controller")), this);
			 p.start();
		} catch (HostNotFoundException e) {
			Msg.critical("The controller '"+config.get("controller")+"' for the SimpleReconfigurator scheduler was not found.");
			e.printStackTrace();
		}
	}
	
	@Override
	public void terminate() {
		Msg.debug("Terminating SimpleReconfigurator");
		terminating = true;
	}

	
	/**
	 * Process handling the reconfigurations
	 * @author julien.gossa@unistra.fr
	 */
	protected class SimpleReconfiguratorProcess extends Process {
		
		SimpleReconfigurator scheduler;
				
		/**
		 * SimpleReconfigurator
		 * @param host the host to run this process
		 * @param scheduler the scheduler using this process
		 */
		public SimpleReconfiguratorProcess(Host host, SimpleReconfigurator scheduler) {
			super(host, "SimpleReconfigurator-Controller");
			this.scheduler = scheduler;
		}		
		/**
		 * 
		 * @throws MsgException
		 */
		@Override
		public void main(String[] args) throws MsgException {
			int i = 0;
			Compute compute = scheduler.computeEngine.getCompute();
			while(! scheduler.terminating) {
			
				if (i == 0) {
					waitFor(1);
				} else {
					waitFor(scheduler.delay);
				}
				
				// Scan the instances to find one to migrate
				Object[] instances = compute.describeInstances().toArray();
				if ( instances.length == 0 ) continue;
				Instance instance = null;
				ComputeHost bestHost = null;
				do {
					i = (i+1)%instances.length;
					instance = (Instance) instances[i];
					
					// Check if the instance can be migrated 
					if (!instance.isRunning() || instance.vm().isMigrating() == 1) {
						instance = null;
					} else {
						// and if there is a better place for the scanned instance
						try {
							bestHost = schedule(instance.getInstanceType());
							if (   getWeight(bestHost,instance.getInstanceType()) 
								<= getWeight(scheduler.computeEngine.getComputeHostOf(instance),
											instance.getInstanceType()) ) {
								instance = null;
							}
						} catch (VMSchedulingException e) {
							instance = null;
						}
					}
				} while (i!=0 && instance==null);
				
				if (instance != null) {
					trace.addEvent("reconfiguration", instance.getId());
					//Msg.info("Reconfiguration: "+instance.getId()
					//		+" from "+ computeEngine.getComputeHostOf(instance).getHost().getName()
					//		+" to " + bestHost.getHost().getName());
					ComputeTools.asynchroneLiveMigration(computeEngine,instance, bestHost);
				}
			}
		}
	}
	
}
