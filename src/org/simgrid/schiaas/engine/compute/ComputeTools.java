package org.simgrid.schiaas.engine.compute;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Instance;

/**
 * Gather all tools that are independent from the engine implementation.  
 * @author julien.gossa@unistra.fr
 */
public class ComputeTools {

	/**
	 * Process to handle one liveMigration 
	 * @author julien.gossa@unistra.fr
	 */
	private static class LiveMigrationProcess extends Process {
		private Instance instance;
		private ComputeEngine computeEngine;
		
		protected LiveMigrationProcess(ComputeEngine computeEngine, ComputeHost computeHost, Instance instance) {
			super(computeHost.getHost(), "LiveMigrationProcess:"+computeHost);
			this.computeEngine = computeEngine;
			this.instance = instance;
			
			try {
				this.start();
			} catch(HostNotFoundException e) {
				Msg.critical("Something bad happend in the OffLoadProcess"+e.getMessage());
			}
		}

		public void main(String[] arg0) throws MsgException {
			computeEngine.liveMigration(instance);
		}
	}

	
	/**
	 * Synchrone sequential offloader.
	 * Set the given host to unavailable and offloads its VMs.
	 * Migrations are done sequentially. 
	 * 
	 * @param computeEngine
	 *            The engine to use
	 * @param computeHost
	 *            The host to offload
	 */
	public static void offLoad(ComputeEngine computeEngine, ComputeHost computeHost) {
		
		computeHost.setAvailability(false);
		
		while (!computeHost.getHostedInstances().isEmpty()) {
			Instance instance = computeHost.getHostedInstances().iterator().next();
			try {
				computeEngine.liveMigration(instance);
			} catch (HostFailureException e) {
				Msg.error("An error occurs during the migration of the instance "+instance.getId());
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Asynchrone parallel offloader.
	 * Set the given host to unavailable and offloads its VMs.
	 * 
	 * @param computeHost
	 *            The host to offload
	 */
	public static void parallelOffLoad(ComputeEngine computeEngine, ComputeHost computeHost) {

		computeHost.setAvailability(false);
		
		for (Instance instance: computeHost.getHostedInstances()) {
			new LiveMigrationProcess(computeEngine, computeHost, instance);			
		}
	}
}
