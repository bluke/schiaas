package org.simgrid.schiaas.engine.compute.reconfigurator;

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
import org.simgrid.schiaas.engine.compute.ComputeReconfigurator;
import org.simgrid.schiaas.engine.compute.ComputeTools;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

public class CentralizedReconfigurator extends ComputeReconfigurator {
	
	int delay;
	boolean terminating;
	
	ReconfigurationHeuristic reconfigurationHeuristic;

	public CentralizedReconfigurator(ComputeEngine computeEngine, Map<String, String> config) {
		super(computeEngine, config);

		this.delay = Integer.parseInt(config.get("delay"));
		this.terminating = false;
		
		try {
			//this.reconfigurationHeuristic = (ReconfigurationHeuristic)Class.forName(config.get("heuristic")).newInstance();
			this.reconfigurationHeuristic = (ReconfigurationHeuristic)Class.forName(config.get("heuristic")).getConstructor(ComputeEngine.class, Map.class).newInstance(computeEngine, config);
		} catch (Exception e) {
			Msg.critical("Something wrong happened while loading the reconfiguration heuristic "
					+ config.get("heuristic"));
			e.printStackTrace();
		}	
		
		
		CentralizedReconfiguratorProcess p;
		try {
			 p = new CentralizedReconfiguratorProcess(
					Host.getByName(config.get("controller")), this);
			 p.start();
		} catch (HostNotFoundException e) {
			Msg.critical("The controller '"+config.get("controller")+"' for the SimpleReconfigurator scheduler was not found.");
			e.printStackTrace();
		}
	}
	
	@Override
	public void terminate() {
		Msg.debug("Terminating CentralizedReconfigurator");
		terminating = true;
	}

	
	/**
	 * Process handling the reconfigurations
	 * @author julien.gossa@unistra.fr
	 */
	protected class CentralizedReconfiguratorProcess extends Process {
		
		CentralizedReconfigurator reconfigurator;
				
		/**
		 * CentralizedReconfigurator Process
		 * @param host the host to run this process
		 * @param reconfigurator the scheduler using this process
		 */
		public CentralizedReconfiguratorProcess(Host host, CentralizedReconfigurator reconfigurator) {
			super(host, "SimpleReconfigurator-Controller");
			this.reconfigurator = reconfigurator;
		}		
		/**
		 * 
		 * @throws MsgException
		 */
		@Override
		public void main(String[] args) throws MsgException {

			while(! reconfigurator.terminating) {
			
				waitFor(reconfigurator.delay);
				Msg.info("Reconfiguration");
				reconfigurationHeuristic.computeReconfigurationPlan();
				reconfigurationHeuristic.computeReconfigurationPlan();
			}
		}
	}	
}
