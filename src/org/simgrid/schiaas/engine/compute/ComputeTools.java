package org.simgrid.schiaas.engine.compute;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

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
		private ComputeEngine computeEngine;
		private Instance instance;
		private ComputeHost destination;
		private VMSchedulingException vmSchedulingException;

		/**
		 * Constructor with destination
		 * @param computeEngine the engine handling this migration
		 * @param instance the instance to migrate
		 * @param destination the destination of the migration
		 */
		protected LiveMigrationProcess(ComputeEngine computeEngine, Instance instance, ComputeHost destination) {
			super(computeEngine.getComputeHostOf(instance).getHost(), "LiveMigrationProcess:"+instance.getId());
			this.computeEngine = computeEngine;
			this.instance = instance;
			this.destination = destination;
			this.vmSchedulingException = null;
			
			try {
				this.start();
			} catch(HostNotFoundException e) {
				Msg.critical("Something bad happend in the liveMigrationProcess"+e.getMessage());
			}
		}

		/**
		 * Constructor without destination, to let the scheduler decide
		 * @param computeEngine the engine handling this migration
		 * @param instance the instance to migrate
		 */
		protected LiveMigrationProcess(ComputeEngine computeEngine, Instance instance) {
			this(computeEngine, instance, null);
		}
				
		/**
		 * Simply execute the migration.
		 */
		public void main(String[] arg0) throws MsgException {
			if (destination != null) {
				computeEngine.liveMigration(instance, destination);
			}
			else {
				try {
					computeEngine.liveMigration(instance);
				} catch (VMSchedulingException e) {
					this.vmSchedulingException = e;
				}
			}
		}
		
	}

	/**
	 * Execute a live migration asynchronously to a given destination
	 * @param computeEngine the engine handling this migration
	 * @param instance the instance to migrate
	 * @param destination the destination of the migration
	 */
	public static void asynchroneLiveMigration(ComputeEngine computeEngine, Instance instance, ComputeHost destination) {
		new LiveMigrationProcess(computeEngine, instance, destination);
	}

	/**
	 * Execute a live migration asynchronously, letting the scheduler decide of the destination 
	 * @param computeEngine the engine handling this migration
	 * @param instance the instance to migrate
	 */
	public static void asynchroneLiveMigration(ComputeEngine computeEngine, Instance instance) {
		new LiveMigrationProcess(computeEngine, instance);
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
	 * @throws VMSchedulingException whenever the scheduling of one instance is not possible. Stops the offload.
	 */
	public static void offLoad(ComputeEngine computeEngine, ComputeHost computeHost) throws VMSchedulingException {
		
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
	 * @throws VMSchedulingException whenever the scheduling of one instance is not possible. Stops the offload.
	 */
	public static void parallelOffLoad(ComputeEngine computeEngine, ComputeHost computeHost) throws VMSchedulingException {

		computeHost.setAvailability(false);
		
		for (Instance instance: computeHost.getHostedInstances()) {
			LiveMigrationProcess lmp = new LiveMigrationProcess(computeEngine, instance);
			if (lmp.vmSchedulingException != null) {
				throw lmp.vmSchedulingException;
			}
		}
	}
	
	/**
	 * Spawn a process to wait for the given instance to run
	 * before starting the given process 
	 * 
	 * @param computeEngine the engine to use
	 * @param instance the instance to wait for running
	 * @param process the process to start afterward
	 * @throws HostNotFoundException just like process.start()
	 */
	public static void waitForRunningAndStart(final ComputeEngine computeEngine, final Instance instance, final Process process) throws HostNotFoundException {
		class WaitProcess extends Process {
			public WaitProcess() {
				super(computeEngine.getComputeHostOf(instance).getHost(), "WaitProcess-"+process.getName());
			}
			@Override
			public void main(String[] arg0) throws MsgException {
				while(instance.vm().isRunning() == 0 ) {
					if (instance.isTerminating()) return;
					waitFor(1);
				}
				try {
					process.start();
				} catch (HostNotFoundException e) {
					Msg.critical("Something went wrong while trying start "+process.getName()
						+"after waiting for "+instance.getId()+" to run."); 
					e.printStackTrace();
				}

			}
		}
		
		Process waitProcess = new WaitProcess();

		waitProcess.start();
	}
	
}
