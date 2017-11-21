package org.simgrid.schiaas.engine.compute.reconfigurator.simpleheuristic;

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
import org.simgrid.schiaas.engine.compute.ComputeReconfigurator.ReconfigurationHeuristic;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.engine.compute.scheduler.simplescheduler.SimpleScheduler;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

public class SimpleHeuristic extends ReconfigurationHeuristic {
	
	/**
	 * Enumerates the possible scheduler types
	 */
	protected static enum TYPE {
		BALANCER, CONSOLIDATOR;
	};
	
	protected TYPE type;

	protected ComputeEngine computeEngine;
	protected Compute compute;
	protected Instance instance;
	protected ComputeHost bestHost;
	protected SimpleScheduler simpleScheduler;
	protected int i;

	public SimpleHeuristic(ComputeEngine computeEngine, Map<String, String> config) {
		super(computeEngine, config);
		this.computeEngine = computeEngine;
		this.compute = computeEngine.getCompute();
		this.type = TYPE.valueOf(config.get("type").toUpperCase());
		this.simpleScheduler = new SimpleScheduler(computeEngine, config);
		this.i = 0;
	}

	@Override
	public void computeReconfigurationPlan() {
		Msg.info("compute reconfiguration plan");
		
		instance = null;
		bestHost = null;

		Object[] instances = compute.describeInstances().toArray();
		if ( instances.length == 0 ) return;

		do {
			i = (i+1)%instances.length;
			instance = (Instance) instances[i];
			
			// Check if the instance can be migrated 
			if (!instance.isRunning() || instance.vm().isMigrating() == 1) {
				instance = null;
			} else {
				// and if there is a better place for the scanned instance
				try {
					bestHost = simpleScheduler.schedule(instance.getInstanceType());
					if (   simpleScheduler.getWeight(bestHost,instance.getInstanceType()) 
						<= simpleScheduler.getWeight(computeEngine.getComputeHostOf(instance),
									instance.getInstanceType()) ) {
						instance = null;
					}
				} catch (VMSchedulingException e) {
					instance = null;
				}
			}
		} while (i!=0 && instance==null);
	}


	@Override
	public void applyReconfigurationPlan() {
		Msg.info("apply reconfiguration plan");
		if (instance != null) {

			Msg.info("Reconfiguration: "+instance.getId()
					+" from "+ computeEngine.getComputeHostOf(instance).getHost().getName()
					+" to " + bestHost.getHost().getName());
			ComputeTools.asynchroneLiveMigration(computeEngine,instance, bestHost);
		}		
	}
	
}
