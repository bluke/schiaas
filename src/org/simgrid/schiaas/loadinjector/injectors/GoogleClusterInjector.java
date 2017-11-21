package org.simgrid.schiaas.loadinjector.injectors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.exceptions.MissingConfigException;
import org.simgrid.schiaas.exceptions.VMSchedulingException;
import org.simgrid.schiaas.loadinjector.AbstractInjector;
import org.simgrid.schiaas.loadinjector.LoadedInstance;


public class GoogleClusterInjector extends AbstractInjector {
	
	/** Difference between the dates in the Google traces and in the simulation 
	 * e.g. with dateDecal= 10, the events at date 0 in the Google traces will occur at clock 10 in the simulation */
	protected Double dateDecal;

	/** The value of the CPURequest in the simulation when it is 1.0 in the Google traces */
	protected int CPURequestMax;
	/** The value of the RAMRequest in the simulation when it is 1.0 in the Google traces */
	protected int RAMRequestMax;
	/** The value of the DiskRequest in the simulation when it is 1.0 in the Google traces */
	protected int diskRequestMax;
	
	/** The id of the image of the instances */
	private final String imageId;

	/** Reader of the task_events file */
	private BufferedReader taskEventsBR;
	
	private TaskUsageInjectorProcess taskUsageInjectorProcess;  
	
	private HashMap<String, LoadedInstance> loadedInstances;
	
	public GoogleClusterInjector(String id, Cloud cloud, Map<String,String> config) throws MissingConfigException {
		super(id, cloud, config); 
		
		try {
			this.dateDecal = Double.parseDouble(config.get("date_decal"));
			this.imageId = config.get("image_id");
			this.CPURequestMax = Integer.parseInt(config.get("CPURequestMax"));
			this.RAMRequestMax = Integer.parseInt(config.get("RAMRequestMax"));
			this.diskRequestMax = Integer.parseInt(config.get("diskRequestMax"));
			config.get("task_events_filename");

		} catch (Exception e) {
			throw new MissingConfigException("GoogleClusterInjector", "dateDecal, imageId, CPURequestMax, RAMRequestMax, diskRequestMax, task_events_filename [task_usage_filename]");
		}
			
		try {
			taskEventsBR = new BufferedReader(new FileReader(config.get("task_events_filename")));
		} catch (FileNotFoundException e) {
			Msg.critical("Error while opening the Google Cluster trace events file "+config.get("task_events_filename"));
			e.printStackTrace();
		}
		
		this.loadedInstances = new HashMap<String, LoadedInstance>();		
	}
	
	
	@Override
	public void run() throws HostFailureException {

		Msg.info("Starting the GoogleClusterInjector "+id);
		if (config.get("task_usage_filename") != null) {			
			Msg.info("Starting the TaskUsageProcess of the GoogleClusterInjector "+id);			

			this.taskUsageInjectorProcess = new TaskUsageInjectorProcess(config.get("task_usage_filename"));
			this.taskUsageInjectorProcess.start();
		}
		
		String taskEventLine;
		String[] taskEventLog;
		double taskEventDate;

		try {

			while ((taskEventLine = taskEventsBR.readLine()) != null) {
				taskEventLog = taskEventLine.split(",");
				taskEventDate = Double.parseDouble(taskEventLog[0]);
				
				if (this.dateDecal + taskEventDate < Msg.getClock() ) {
					Msg.warn("The injector "+id+"is late for the injection of the log line "+ taskEventLine);
				} else {
					Process.getCurrentProcess().waitFor(
						this.dateDecal + taskEventDate - Msg.getClock() );
				}

				String taskId = taskEventLog[2]+'-'+taskEventLog[3];
				int eventType = Integer.parseInt(taskEventLog[5]);
				double CPURequest = Double.parseDouble(taskEventLog[9]) * CPURequestMax;
				int RAMRequest = (int) Double.parseDouble(taskEventLog[10]) * RAMRequestMax;
				int diskRequest = (int) Double.parseDouble(taskEventLog[11]) * diskRequestMax;

				switch(eventType) {
				case 0: // Submit, not supported
					break;
				case 1: // Schedule, = starting a new instance
					try {
						loadedInstances.put(taskId,
							new LoadedInstance(cloud.getCompute(), this.imageId, CPURequest, RAMRequest, diskRequest));
						Msg.info("Injecting "+taskId);
					} catch (VMSchedulingException e) {
						Msg.warn("The instance corresponding to the Google Traces job-task id "+taskId+" was not ran becaause "+e.getMessage());
					}
					break;
				case 2: // Evict
				case 3: // Fail
				case 4: // Finish
				case 5: // Kill
				case 6: // Lost, = terminate instance
					loadedInstances.remove(taskId).terminate();
					break;
				case 7: // Update pending, not supported
				case 8: // Update running, not supported
					break;

				}
			}		
		} catch (IOException e) {
			Msg.critical("Something wet wrong while the injector "+id+" was reading the Google Cluster trace file");
			e.printStackTrace();
		}
		
		// terminating
		if (this.taskUsageInjectorProcess != null) {
			this.taskUsageInjectorProcess.kill();
		}
		for (LoadedInstance li : loadedInstances.values()) {
			li.terminate();
		}
		this.trace.addEvent("instances_count", ""+0);
		
	}
	
	private class TaskUsageInjectorProcess extends Process {
		
		/** Reader of the task_usage file */
		private BufferedReader taskUsageBR;
		

		public TaskUsageInjectorProcess(String taskUsageFilename) {
			super(Process.getCurrentProcess().getHost(), "GoogleClusterInjectorTaskUsageProcess");
			
			try {
				this.taskUsageBR = new BufferedReader(new FileReader(taskUsageFilename));
			} catch (FileNotFoundException e) {
				Msg.critical("Error while opening the Google Cluster trace usage file "+taskUsageFilename);
				e.printStackTrace();
			}
		}
					
		@Override
		public void main(String[] arg0) throws MsgException {

			String taskUsageLine;
			String[] taskUsageLog;
			double taskUsageDate;

			try {
				while ((taskUsageLine = this.taskUsageBR.readLine()) != null) {
					taskUsageLog = taskUsageLine.split(",");
					taskUsageDate = Double.parseDouble(taskUsageLog[0]);
										
					if (dateDecal + taskUsageDate < Msg.getClock() ) {
						Msg.warn("The injector "+id+"is late for the injection of the log line "+ taskUsageLine);
					} else {
						Process.getCurrentProcess().waitFor(
							dateDecal + taskUsageDate - Msg.getClock() );
					}

					String taskId = taskUsageLog[2]+'-'+taskUsageLog[3];
					double GCCPURate = Double.parseDouble(taskUsageLog[5]);
					
					LoadedInstance li = loadedInstances.get(taskId);
					try {
						// TODO: pas bon
						li.setLoad(GCCPURate /  100);
					} catch (NullPointerException e) {
						Msg.warn("The instance "+taskId+" was not found to change the resource usage. It might have already been terminated");
					}
				}
			} catch (IOException e) {
				Msg.critical("Something wet wrong while the injector "+id+" was reading the Google Cluster task usage file");
				e.printStackTrace();
			}
		}
	}	
}
