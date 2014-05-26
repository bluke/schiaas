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
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.msg.Process;

import simschlouder.util.SimSchloudException;


public class SchloudNode extends Process {
	
	public enum STATE {PENDING,CLAIMED,IDLE,BUSY,TERMINATED};

	public String instanceId;
	
	protected SchloudCloud cloud;
	
	protected LinkedList<SchloudTask> queue;
	protected LinkedList<SchloudTask> completedQueue;
	
	protected SchloudTask currentSchloudTask;
	
	protected STATE state;
	
	protected double idleDate;
	protected double pendingDate;
	protected double terminatedDate;
	protected double bootDate;
	protected double bootTimePrediction;
	
	protected double speed;
	
	protected class SchloudNodeController extends Process {
		private SchloudNode schloudNode;
		protected SchloudNodeController(SchloudNode schloudNode) {
			super(SchloudController.host,schloudNode.id+"_SchloudNodeController");
			this.schloudNode = schloudNode;
		}
		public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {
			try {
				// Wait for at least the predicted boottime. Can be optimized.
				Process.currentProcess().waitFor(schloudNode.bootTimePrediction);
				while(cloud.compute.describeInstance(instanceId).isRunning() == 0)
				{
					Msg.info("wait for boot of "+instanceId);
					Process.currentProcess().waitFor(5);
				}
				schloudNode.setState(STATE.IDLE);
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

	
	protected SchloudNode(String instanceId, SchloudCloud cloud) {
		super(cloud.compute.describeInstance(instanceId), instanceId+"_SchloudNode",null);
		this.instanceId = instanceId;
		this.cloud=cloud;
		
		speed = cloud.compute.describeInstance(instanceId).getSpeed();
		
		this.queue = new LinkedList<SchloudTask>();
		this.completedQueue = new LinkedList<SchloudTask>();
		
		idleDate = Msg.getClock()+cloud.getBootTimePrediction();
		
		bootTimePrediction=cloud.getBootTimePrediction();
		
		//Msg.info("idleDate" + idleDate);
		setState(STATE.PENDING);
		
	}
	
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
	
	public void setState(STATE state) {
		Msg.verb("SchloudNode " + name + " state set to " + state);
		
		if (this.state == STATE.IDLE && state != STATE.IDLE) {
			SchloudController.idleNodesCount--;
		}
		if (this.state != STATE.IDLE && state == STATE.IDLE) {
			SchloudController.idleNodesCount++;
		}
		
		switch (state) {
		case IDLE: idleDate=Msg.getClock();
			break;
		case PENDING: pendingDate=Msg.getClock();
			break;
		case TERMINATED: terminatedDate=Msg.getClock();
			break;
		}
		
		
		this.state = state;
	}
	
	public double getUptime() {
		if (state == STATE.TERMINATED) {
			return terminatedDate - pendingDate;
		}
		return Msg.getClock() - pendingDate;
	}
	
	public double getUpTimeToIdle() {
		if (isIdle()) {
			return Msg.getClock() - pendingDate;
		}
		return  idleDate - pendingDate;
	}
	
	public double getIdleDate() {
		return idleDate;
	}
	
	public double getRemainingIdleTime(){
		return (SchloudController.time2BTU(getUpTimeToIdle())*SchloudController.schloudCloud.getBtuTime())-(getUpTimeToIdle());
	}
	
	public double getRuntimePrediction(SchloudTask schloudTask) {
		return schloudTask.duration/this.speed;
	}
	
	public void enqueue(SchloudTask task) {
		queue.add(task);
		idleDate+=task.getWalltimePrediction();
		if ( state == STATE.IDLE ) {
			setState(STATE.CLAIMED);
		}
	}
	
	public boolean dequeue(SchloudTask task) {
		if (task.state!=SchloudTask.STATE.COMPLETE) return false;
		
		queue.remove(task);
		idleDate-=task.getWalltimePrediction();
		
		return true;
	}


	
	protected void processQueue() {
		SchloudTaskController stc = new SchloudTaskController(this);
		try {
			stc.start();
		} catch (HostNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void main(String[] args) throws TransferFailureException, HostFailureException, TimeoutException {

		bootDate=Msg.getClock();
		 
		while(true) {
			// Receiving the command
			Task task = Task.receive(getMessageBox());
			try {
				task.execute();
			} catch (TaskCancelledException e) {
				Msg.info("Something bad happened in SimSchlouder: "+e.getStackTrace());
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
				Msg.info("Something bad happened in SimSchlouder: "+e.getStackTrace());
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
			currentSchloudTask.getCompleteTask().send(getMessageBox());
		}
	}
	

	protected String getMessageBox() {
		return "SSMB"+msgName(); 
	}

	public boolean isIdle() {
		return (state == STATE.IDLE);
		//return (instance.getCount()==0);
		//return (idleDate <= Msg.getClock()); 
	}
	
	public String toString() {
		String s = instanceId.toString();
		for (SchloudTask st : completedQueue) {
			s += "\n"+st;
		}
		return s;
	}

	public void writeJSON(BufferedWriter out) throws IOException, SimSchloudException {
		out.write("\t\t\"instance_id\": \""+name+"\",\n");
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

	public double getSpeed() {
		return speed;
	}
	
}
