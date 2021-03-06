/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */
package simschlouder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.exceptions.VMSchedulingException;

import simschlouder.util.SimSchlouderException;


/**
 * Represents a worker node in the SimSchlouder system
 * @author julien.gossa@unistra.fr
 *
 */
public class SchloudNode extends Process implements Comparable<SchloudNode>{
	
	/** 
	 * Enumerates the states of SimSchlouder worker nodes
	 * @author julien.gossa@unistra.fr
	 */
	public enum STATE {FUTURE,PENDING,CLAIMED,IDLE,BUSY,TERMINATED};

	/** The instance of this */
	public Instance instance;
	
	/** The index of the instance */
	public int index;
	
	/** The current index of tasks */
	static int currentIndex = 0;
	
	/** Cloud where this node is */
	protected SchloudCloud cloud;
	
	/** Local tasks queue of this node */
	protected LinkedList<SchloudTask> queue;	
	/** Local completed tasks queue of this node */
	protected LinkedList<SchloudTask> completedQueue;
	
	/** Currently running task */
	protected SchloudTask currentSchloudTask;
	
	/** Current state of this node */
	protected STATE state;

	/** The date when this node becomes future */
	protected double futureDate;	
	/** The date when this node becomes pending */
	protected double pendingDate;	
	/** The date when this node becomes idle */
	protected double idleDate;
	/** The date when this node becomes terminated */
	protected double terminatedDate;
	/** The date when this node booted */
	protected double bootDate;
	/** The prediction of the boottime of this node */
	protected double bootTimePrediction;
	
	/** CPU speed of the node. Used to convert task runtime to MSG's compute duration */
	protected double speed;
	
	
	/**
	 * A process to wait for the instance to run.
	 * Patch until the boot is properly managed from simgrid side.
	 * @author julien.gossa@unistra.fr
	 */
	protected class SchloudNodeController extends Process {
		private SchloudNode schloudNode;
		private double bootTimePrediction;
		private SchloudBootInfos schloudBootInfos;

		protected SchloudNodeController(SchloudNode schloudNode) {
			super(SchloudController.controller,schloudNode.getName()+"_SchloudNodeController");
			this.schloudNode = schloudNode;
			this.bootTimePrediction = SchloudController.schloudCloud.getBootTimePrediction();

			//TODO: check that
			this.schloudNode.futureDate = Msg.getClock();
			
			// Whenever there are some boot infos to be forced
			if (!SchloudController.schloudCloud.schloudBootInfos.isEmpty()) {
				this.schloudBootInfos = SchloudController.schloudCloud.schloudBootInfos.removeFirst();				
			} else {
				this.schloudBootInfos = new SchloudBootInfos();
			}
			
			if (this.schloudBootInfos.bootTime == null)
				this.schloudBootInfos.bootTime = this.bootTimePrediction;
			if (this.schloudBootInfos.monitoringTime == null)
				this.schloudBootInfos.monitoringTime = SchloudController.schloudCloud.monitoringPrevisionTime;
			
			//TODO: check that
			if (this.schloudBootInfos.provisioningDate != null )
				this.schloudNode.futureDate = schloudBootInfos.provisioningDate;
			
			// Adding the monitoring task 
			//TODO: simulate the communications
			if (cloud.monitoringPrevisionTime != null) {
				SchloudTask task = new SchloudTask(
						"monitoring-"+instance.getId(), 
						cloud.monitoringPrevisionTime, 
						this.schloudBootInfos.monitoringTime, 
						0, 0, 
						new Vector<SchloudTask>());
				task.setState(SchloudTask.STATE.SCHEDULED);
				SchloudController.setTaskToNode(task, this.schloudNode);
			}

		}
		
		public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
			try {
				schloudNode.setState(STATE.FUTURE);
				idleDate = Msg.getClock()+bootTimePrediction;
				double provisioningDelay = 0;
				
				// Wait for the provisioning date
				if (schloudBootInfos.provisioningDate != null) {
					provisioningDelay = schloudBootInfos.provisioningDate-Msg.getClock();
					if (provisioningDelay < 0) {
						Msg.warn("The provisioning date of "+schloudNode.instance.getId()+" is in the past.");
						provisioningDelay = 0;
					}
				
					Msg.verb(instance.getId()+" waits for provisioning: "+provisioningDelay);
					Process.getCurrentProcess().waitFor(provisioningDelay);
				}
				
				schloudNode.setState(STATE.PENDING);
				//idleDate += provisioningDelay;
								
				// Wait for at least the predicted boottime. Can be optimized.
				Msg.verb(instance.getId()+" waits for boot: "+schloudBootInfos.bootTime );
				Process.getCurrentProcess().waitFor(schloudBootInfos.bootTime);	
				while(!instance.isRunning())
				{
					Msg.verb(instance.getId()+" boot delayed");
					Process.getCurrentProcess().waitFor(1);
				}
				
				idleDate += provisioningDelay + schloudBootInfos.bootTime - bootTimePrediction;
				
				Msg.verb(instance.getId()+" booted ");
				bootDate=Msg.getClock();
				//if (!SimSchlouder.validation) 
				SchloudController.schloudCloud.decrementBootCount();
				
				// Wait for the lag time
				if ( schloudBootInfos.lagTime != null ) {
					Msg.verb(instance.getId()+" wait for lag time "+schloudBootInfos.lagTime);
					waitFor(schloudBootInfos.lagTime);
				}
				
				
			} catch (HostFailureException e) {
				Msg.critical("Something bad happened is SimSchlouder: The host of "+instance.getId()+" was not found.");
				e.printStackTrace();
			}
			
		

		}
	}

	/**
	 * A Schlouder Node, basically peeking tasks and executing them.
	 * @param instance The instance of this node
	 * @param cloud The cloud of this node
	 */
	protected SchloudNode(Instance instance, SchloudCloud cloud) {
		super(instance.vm(), instance.getId()+"_SchloudNode",null);
		this.index = currentIndex++;
		this.instance = instance;
		this.cloud=cloud;

		speed = instance.vm().getSpeed();

		this.queue = new LinkedList<SchloudTask>();
		this.completedQueue = new LinkedList<SchloudTask>();

		bootTimePrediction=cloud.getBootTimePrediction();
		
		setState(STATE.FUTURE);
	}
	
	/**
	 * Start a new node.
	 * @param cloud the cloud to start the new node.
	 * @return a new worker node.
	 * @throws VMSchedulingException 
	 */
	public static SchloudNode startNewNode(SchloudCloud cloud) throws VMSchedulingException {
		
		Instance instance = cloud.compute.runInstance(SchloudController.imageId, SchloudController.instanceTypeId);
		
		SchloudNode schloudNode = new SchloudNode(instance,cloud);

		SchloudNodeController schloudNodeController = schloudNode.new SchloudNodeController(schloudNode); 
		schloudNodeController.start();
		
		return schloudNode;
	}

	
	@Override
    public int compareTo(SchloudNode other){
		if (this.state != STATE.FUTURE && other.state != STATE.FUTURE)
			if (this.pendingDate != other.pendingDate) 
				return (int)(this.pendingDate - other.pendingDate);
			else
				return (int)(other.index - this.index);
		
		if (this.state != STATE.FUTURE) return 1;
		if (other.state != STATE.FUTURE) return -1;
		
		return (int)(this.index - other.index);
	}
	
	/**
	 * Get the state of this node.
	 * @return state of this node.
	 */
	public STATE getState() {
		return this.state;
	}
	
	/**
	 * Set the state of this node.
	 * @param state the new state of this node.
	 */
	public void setState(STATE state) {
		Msg.verb("SchloudNode " + getName() + " state set to " + state);
		
		if (this.state == STATE.IDLE && state != STATE.IDLE) {
			SchloudController.idleNodesCount--;
		}
		if (this.state != STATE.IDLE && state == STATE.IDLE) {
			SchloudController.idleNodesCount++;
		}
		
		switch (state) {
		case FUTURE: pendingDate=Msg.getClock();
			break;
		case PENDING: pendingDate=Msg.getClock();
			break;
		case TERMINATED: terminatedDate=Msg.getClock();
			break;
		default:
			break;
		}
		
		this.state = state;
	}
	
	

	/**
	 * @return the speed of this node
	 */
	public double getSpeed() {
		return speed;
	}

	
	/**
	 * @return the future date of this node
	 */
	public double getFutureDate() {
		return futureDate;
	}

	/**
	 * @return the pending date of this node
	 */
	public double getPendingDate() {
		return pendingDate;
	}

	
	/**
	 * 
	 * @return the up-time of this node (from pending to now or terminated)
	 */
	public double getUptime() {
		if (state == STATE.TERMINATED) {
			return terminatedDate - pendingDate;
		}
		return Msg.getClock() - pendingDate;
	}
	
	/**
	 * 
	 * @return the time from the boot to the idle state.
	 */
	public double getUpTimeToIdle() {
		// Da bug
		if (SimSchlouder.validation && state == STATE.FUTURE)
			return (getIdleDate() - bootTimePrediction) - pendingDate;
		if (SimSchlouder.validation && state == STATE.CLAIMED)
			return Msg.getClock() - pendingDate;

		return getIdleDate() - pendingDate;
	}
	
	/**
	 * 
	 * @return the date when this node becomes idle
	 */
	public double getIdleDate() {		
		if (state == STATE.IDLE) return Msg.getClock();
		return idleDate;
	}

	/**
	 * @return the full idle time on this node 
	 */
	public double getFullIdleTime(){
		return ( (SchloudController.time2BTU(getIdleDate() - pendingDate) * cloud.getBtuTime())
				- (idleDate - pendingDate) );
	}

	
	/**
	 * 
	 * @return the remaining idle time on this node 
	 */
	public double getRemainingIdleTime(){			
		return (  (SchloudController.time2BTU(getUpTimeToIdle() + cloud.getShutdownMargin()) * cloud.getBtuTime()) 
				- (getUpTimeToIdle() + cloud.getShutdownMargin()) );
	}	
	
	/**
	 * @param task a SchloudTask
	 * @return the remaining idle time on this node 
	 * if the task is assigned to this node 
	 */
	public double getRemainingIdleTime(SchloudTask task){
		// -0.1 to simulate the strict equality as in schlouder
		return (  (SchloudController.time2BTU(getUpTimeToIdle() + cloud.getShutdownMargin() + task.walltimePrediction) * cloud.getBtuTime()) 
				- (getUpTimeToIdle() + cloud.getShutdownMargin() + task.walltimePrediction ) );
	}

	
	/**
	 * 
	 * @param schloudTask the task 
	 * @return the prediction of runtime of the task on this node
	 */
	public double getRuntimePrediction(SchloudTask schloudTask) {
		return schloudTask.duration/this.speed;
	}
	
	/**
	 * Enqueue a task into this node queue
	 * @param task the task to enqueue
	 */
	public void enqueue(SchloudTask task) {
		queue.add(task);
		if ( state == STATE.IDLE ) {
			idleDate = Msg.getClock();
			setState(STATE.CLAIMED);
			this.processQueue();
		}
		idleDate+=task.getWalltimePrediction();
	}
	
	/**
	 * Dequeue a task from this node queue
	 * @param task the task to dequeue
	 * @return false if the task is not completed, true otherwise
	 */
	public boolean dequeue(SchloudTask task) {
		if (task.state!=SchloudTask.STATE.COMPLETE) return false;
		
		queue.remove(task);
		idleDate-=task.getWalltimePrediction();
		
		return true;
	}


	/**
	 * process the queue of this node, by running a SchloudTaskController.
	 */
	protected void processQueue() {
		Msg.verb("Processing queue of "+this);
		SchloudTaskController stc = new SchloudTaskController(this);
		stc.start();
	}
	
	/**
	 * Receives tasks and processes them.
	 */
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
		
		this.processQueue();
		
		while(true) {
			// Receiving the command
			// Msg.info("receive queue :"+this.queue.size()+" "+this.queue.peek().getName());
			Task task = Task.receive(getMessageBox());
			try {
				task.execute();
			} catch (TaskCancelledException e) {
				Msg.critical("Something bad happened in SimSchlouder: "+e.getStackTrace());
			}
			
			// Receiving the input data
			currentSchloudTask.setState(SchloudTask.STATE.INPUTTING);
			if (currentSchloudTask.getInputSize()!=0) {
				switch (SimSchlouder.storageType) {
				case CLOUD :
					SchloudController.schloudCloud.storage.get(currentSchloudTask.getInputData());
					break;
				default :
					Msg.critical("Storage method "+ SimSchlouder.storageType +" not supported.");
				}							
			}
			
			// Executing the task
			currentSchloudTask.setState(SchloudTask.STATE.RUNNING);
			try {
				currentSchloudTask.getRunTask().execute();
			} catch (TaskCancelledException e) {
				Msg.critical("Something bad happened in SimSchlouder: "+e.getStackTrace());
			}
			//Msg.info("\"" + task.getName() + "\" done ");

			// Sending the output data
			currentSchloudTask.setState(SchloudTask.STATE.OUTPUTTING);
			if (currentSchloudTask.getOutputSize()!=0) {
				switch (SimSchlouder.storageType) {
				case CLOUD :
					SchloudController.schloudCloud.storage.put(currentSchloudTask.getOutputData());
					break;
				default :
					Msg.critical("Storage method "+ SimSchlouder.storageType +" not supported.");
				}										
			}
			
			currentSchloudTask.setState(SchloudTask.STATE.FINISHED);
			
			// Send the complete task
			try {
				currentSchloudTask.getCompleteTask().send(getMessageBox());
			} catch (MsgException e) {
				Msg.critical("Something bad happened at the C level: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @return an ID for the MSG message box to communicate with this node
	 */
	protected String getMessageBox() {
		return "SSMB"+getName(); 
	}

	/**
	 * 
	 * @return whether this node is currently idle
	 */
	public boolean isIdle() {
		return (state == STATE.IDLE);
	}
	
	/**
	 * ToString
	 */
	public String toString() {
		String s = instance.getId().toString();
		for (SchloudTask st : completedQueue) {
			s += "\n"+st;
		}
		return s;
	}

	/**
	 * Writes a JSON file for use in later processing.
	 * Unfortunately handmade, because no standard lib was found at the time.
	 * @throws IOException
	 * @throws SimSchlouderException 
	 */
	public void writeJSON(BufferedWriter out) throws IOException, SimSchlouderException {
		out.write("\t\t\"instance_id\": \""+getName()+"\",\n");
		out.write("\t\t\"index\": "+index+",\n");
		out.write("\t\t\"host\": \""+getHost().getName()+"\",\n");
		out.write("\t\t\"start_date\": "+pendingDate+",\n");
		out.write("\t\t\"stop_date\": "+terminatedDate+",\n"); // TERMINATED AND NOT SHUTTINGDOWN
		out.write("\t\t\"boot_time\": "+(bootDate-pendingDate)+",\n");
		out.write("\t\t\"boot_time_prediction\": "+bootTimePrediction+",\n");
		out.write("\t\t\"instance_type\": \"standard\",\n"); 
		out.write("\t\t\"cloud\": \""+SchloudController.schloudCloud.name+"\",\n");
		out.write("\t\t\"jobs\": [\n");
		for (int i=0; i<completedQueue.size(); i++) {
			out.write("\t\t\t{\n");
			completedQueue.get(i).writeJSON(out);
			out.write("\t\t\t}");
			if (i<completedQueue.size()-1) out.write(",");
			out.write("\n");
		}
		out.write("\t\t]\n");
	}

	
}
