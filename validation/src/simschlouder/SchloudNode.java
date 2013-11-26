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
import org.simgrid.simiaas.api.Compute;
import org.simgrid.simiaas.api.Instance;

public class SchloudNode extends Process {
	
	public enum STATE {PENDING,CLAIMED,IDLE,BUSY,TERMINATED};

	public Instance instance;
	
	protected Cloud cloud;
	
	protected LinkedList<SchloudTask> queue;
	protected LinkedList<SchloudTask> completedQueue;
	
	protected SchloudTask currentSchloudTask;
	
	protected STATE state;
	
	protected double idleDate;
	
	protected SchloudNode(Instance instance, Cloud cloud) {
		super(instance,instance.getName()+"_SchloudNode",null);
		this.instance = instance;
		this.cloud=cloud;
		
		this.queue = new LinkedList<SchloudTask>();
		this.completedQueue = new LinkedList<SchloudTask>();
		
		idleDate = Msg.getClock()+cloud.getBootTimePrediction();
		//Msg.info("idleDate" + idleDate);
		setState(STATE.PENDING);
	}
	
	public static SchloudNode startNewNode(Cloud cloud) {
		Instance instance = Compute.runInstance(SchloudController.schloudNodeImage, cloud.name);
		if (instance==null) return null;
		
		SchloudNode node = new SchloudNode(instance,cloud);
		
		try {
			node.start();
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened is SimSchlouder: The host "+instance.getHost().getName()+" was not found.");
			e.printStackTrace();
		}
		return node;
	}
	
	public void setState(STATE state) {
		Msg.verb("SchloudNode " + name + " state set to " + state);
		
		if (this.state == STATE.IDLE && state != STATE.IDLE) {
			SchloudController.idleNodesCount--;
		}
		if (this.state != STATE.IDLE && state == STATE.IDLE) {
			SchloudController.idleNodesCount++;
		}
		
		this.state = state;
	}
	
	public double getUptime() {
		if (instance.getState() == Instance.STATE.TERMINATED) {
			return instance.getDateOfLast(Instance.STATE.TERMINATED)-instance.getDateOfFirst(Instance.STATE.PENDING);
		}
		return Msg.getClock()-instance.getDateOfFirst(Instance.STATE.PENDING);
	}
	
	public double getUpTimeToIdle() {
		if (isIdle()) {
			return Msg.getClock()-instance.getDateOfFirst(Instance.STATE.PENDING);
		}
		return  idleDate - instance.getDateOfFirst(Instance.STATE.PENDING);
	}
	
	public double getIdleDate() {
		return idleDate;
	}
	
	public void enqueue(SchloudTask task) {
		queue.add(task);
		idleDate+=task.predictedRuntime;
		if ( state == STATE.IDLE ) {
			setState(STATE.CLAIMED);
		}
	}
	
	public boolean dequeue(SchloudTask task) {
		if (task.state!=SchloudTask.STATE.COMPLETE) return false;
		
		queue.remove(task);
		idleDate-=task.predictedRuntime;
		
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

		//Msg.info("SchloudNode "+name+" waiting for its instance"+instance.getName()+ " to boot");
		instance.waitForRunning();

		while(true) {
			Task task = Task.receive(getMessageBox());
			currentSchloudTask.setState(SchloudTask.STATE.RUNNING);

			//double runDate=Msg.getClock();
			//double compDur=task.getComputeDuration();
			//Msg.info("Received \"" + task.getName() +  "\". Processing it.");
			try {
				task.execute();
			} catch (TaskCancelledException e) {

			}
			//Msg.info("\"" + task.getName() + "\" done ");
			
			currentSchloudTask.setState(SchloudTask.STATE.FINISHED);
			
			//Msg.info("RunTime : "+ compDur + " " + (Msg.getClock()-runDate));
			
			currentSchloudTask.getOutputTask().send(getMessageBox());
			
			
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
		String s = instance.toString();
		for (SchloudTask st : completedQueue) {
			s += "\n"+st;
		}
		return s;
	}

	public void writeJSON(BufferedWriter out) throws IOException {
		out.write("\t\t\"instance_id\": \""+name+"\",\n");
		out.write("\t\t\"host\": \""+getHost().getName()+"\",\n");
		out.write("\t\t\"start_date\": "+instance.getDateOfFirst(Instance.STATE.PENDING)+",\n");
		out.write("\t\t\"stop_date\": "+instance.getDateOfLast(Instance.STATE.TERMINATED)+",\n"); // TERMINATED AND NOT SHUTTINGDOWN
		out.write("\t\t\"boot_time\": "+(instance.getDateOfFirst(Instance.STATE.RUNNING)-instance.getDateOfFirst(Instance.STATE.PENDING))+",\n");
		out.write("\t\t\"predicted_boot_time\": "+(instance.getDateOfFirst(Instance.STATE.RUNNING)-instance.getDateOfFirst(Instance.STATE.PENDING))+",\n"); // NOT THE PREDICTION USED ACTUALLY
		out.write("\t\t\"instance_type\": \"standard\",\n"); // NOT THE PREDICTION USED ACTUALLY
		out.write("\t\t\"scheduling_strategy\": \""+SchloudController.strategy.getName()+"\",\n"); // NOT THE PREDICTION USED ACTUALLY
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
