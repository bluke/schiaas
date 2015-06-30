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

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.NativeException;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;

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

	/** Id of the instance running this worker node */
	public String instanceId;

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
		private double bootTime;
		private double provisioningDate;
		private double lagTime;
		protected SchloudNodeController(SchloudNode schloudNode) {
			super(SchloudController.host,schloudNode.getName()+"_SchloudNodeController");
			this.schloudNode = schloudNode;
			this.bootTimePrediction = SchloudController.schloudCloud.getBootTimePrediction();
			this.bootTime = this.bootTimePrediction;
			this.provisioningDate = Msg.getClock();
			
			// Whenever there are some boot times to be forced
			if (!SchloudController.schloudCloud.bootTimes.isEmpty()) {
				this.bootTime = SchloudController.schloudCloud.bootTimes.removeFirst();
			}
			// Whenever there are some provisioning dates to be forced
			//Msg.info("remove prov date: "+SchloudController.schloudCloud.provisioningDates.peekFirst()+ " " + SchloudController.schloudCloud.provisioningDates.size());
			if (!SchloudController.schloudCloud.provisioningDates.isEmpty()) 			
				this.provisioningDate = SchloudController.schloudCloud.provisioningDates.removeFirst();
			if (!SchloudController.schloudCloud.lagTimes.isEmpty()) 			
				this.lagTime = SchloudController.schloudCloud.lagTimes.removeFirst();
		}
		
		public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
			try {
				schloudNode.setState(STATE.FUTURE);
				idleDate = Msg.getClock()+bootTimePrediction;
				
				// Wait for the provisioning date
				double provisioningDelay = provisioningDate-Msg.getClock();
				if (provisioningDelay < 0) provisioningDelay = 0;
				Msg.verb(instanceId+" waits for provisioning: "+provisioningDelay);
				Process.getCurrentProcess().waitFor(provisioningDelay);
				
				schloudNode.setState(STATE.PENDING);
				//idleDate += provisioningDelay;
				
				// This imitates a Schlouder bug
				if (SimSchlouder.validation) SchloudController.schloudCloud.resetBootCount();
				
				// Wait for at least the predicted boottime. Can be optimized.
				Msg.verb(instanceId+" waits for boot: "+bootTime);
				Process.getCurrentProcess().waitFor(bootTime);				
				while(cloud.compute.describeInstance(instanceId).isRunning() == 0)
				{
					Msg.verb(instanceId+" boot delayed");
					Process.getCurrentProcess().waitFor(1);
				}
				// Should be IDLE if no job are enqueued at this time
				schloudNode.setState(STATE.CLAIMED);
				idleDate += provisioningDelay + bootTime - bootTimePrediction;
				
				Msg.verb(instanceId+" booted ");
				bootDate=Msg.getClock();
				if (!SimSchlouder.validation) SchloudController.schloudCloud.decrementBootCount();
				
				// Wait for the lag time
				Msg.info(instanceId+" wait for lag time "+lagTime);
				waitFor(lagTime);
				
				
			} catch (HostFailureException e) {
				Msg.critical("Something bad happened is SimSchlouder: The host of "+instanceId+" was not found.");
				e.printStackTrace();
			}
			
		try {
			schloudNode.start();
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened is SimSchlouder: The host of "+instanceId+" was not found.");
			e.printStackTrace();
		}

		}
	}

	/**
	 * A Schlouder Node, basically peeking tasks and executing them.
	 * @param instanceId
	 * @param cloud
	 */
	protected SchloudNode(String instanceId, SchloudCloud cloud) {
		super(cloud.compute.describeInstance(instanceId), instanceId+"_SchloudNode",null);
		this.instanceId = instanceId;
		this.index = currentIndex++;
		this.cloud=cloud;
		
		speed = cloud.compute.describeInstance(instanceId).getSpeed();
		
		this.queue = new LinkedList<SchloudTask>();
		this.completedQueue = new LinkedList<SchloudTask>();
		 		
		bootTimePrediction=cloud.getBootTimePrediction();
		
		setState(STATE.FUTURE);
	}
	
	/**
	 * Start a new node.
	 * @param cloud the cloud to start the new node.
	 * @return a new worker node.
	 */
	public static SchloudNode startNewNode(SchloudCloud cloud) {
		String instanceId = cloud.compute.runInstance(SchloudController.imageId, SchloudController.instanceTypeId);
		if (instanceId==null) return null;
		
		SchloudNode schloudNode = new SchloudNode(instanceId,cloud);

		SchloudNodeController schloudNodeController = schloudNode.new SchloudNodeController(schloudNode); 
		try {
			schloudNodeController.start();
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened is SimSchlouder: The host of "+instanceId+" was not found.");
			e.printStackTrace();
		}
		
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
		case FUTURE: pendingDate=futureDate=Msg.getClock();
			break;
		case PENDING: pendingDate=Msg.getClock();
			break;
		case TERMINATED: terminatedDate=Msg.getClock();
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
		}
		idleDate+=task.getWalltimePrediction();
		//Msg.info("idledate "+instanceId+" "+idleDate+" "+(idleDate-23));
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
		SchloudTaskController stc = new SchloudTaskController(this);
		try {
			stc.start();
		} catch (HostNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Receives tasks and processes them.
	 */
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
		 
		while(true) {
			// Receiving the command
			Task task = Task.receive(getMessageBox());
			try {
				task.execute();
			} catch (TaskCancelledException e) {
				Msg.critical("Something bad happened in SimSchlouder: "+e.getStackTrace());
			}

			//Msg.info("Received \"" + task.getName() +  "\". Processing it.");
			
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
			} catch (NativeException e) {
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
		String s = instanceId.toString();
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
		out.write("\t\t\"index\": \""+index+"\",\n");
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
