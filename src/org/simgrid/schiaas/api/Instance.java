package org.simgrid.schiaas.api;

import java.text.DecimalFormat;
import java.util.Vector;
import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.VM;
import org.simgrid.msg.Mutex;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.engine.simiaas.ComputeControllerProcess;
import org.simgrid.schiaas.engine.simiaas.ComputeNodeProcess;
import org.simgrid.schiaas.engine.simiaas.ControlTask;

//ajouter dpIntensity / fonction de la netBW pour la migration


/**
 * Represents an Instance, that is a VM controller by SimIaaS.
 * @author julien.gossa@unistra.fr
 */
public class Instance extends VM {

	/** Enumerates the different state of the instance. */
	public static enum STATE { PENDING, BOOTING, RUNNING, REBOOTING, SUSPENDING, SUSPENDED, RESUMING, SHUTTINGDOWN, TERMINATED };
	
	/** Represents an event for the instance: a date and the state of the instance since this date. */
	public class StateDate { 
		  public final STATE state; 
		  public final double date; 
		  public StateDate(STATE state, double date) { 
		    this.state = state; 
		    this.date = date; 
		  } 
		  public String toString()  {
			  return new DecimalFormat("00000000.00").format(date)+"\t"+state;
		  }
	} 
	
	/** The cloud of the instance. */
	protected CloudTOBEMERGEWITHCOMPUTE cloud;
	
	/** The physical host of the instance. */
	protected CloudTOBEMERGEWITHCOMPUTE.VMHost vmhost;
	
	/** The image of the instance. */
	protected Image image;
	
	/** The hypervisor of the instance. */
	protected ComputeNodeProcess computeNodeProcess;
	
	/** The current state of the instance. */
	protected STATE state;
	
	/** The log of the state changes. */
	protected Vector<StateDate> stateLog;
	
	/** Mutex used to track whether the instance is running or not. */
	public Mutex runningMutex;
	
	/** Mutex used to track whether the image is present on the physical host. */
	public Mutex imageTransferMutex;
	
	
	/**
	 * Constructor to start now.
	 * @TODO Mime VM
	 * @param image The image of the instance.
	 * @param cloud The cloud of the instance.
	 * @param host The physical host of the instance. 
	 * @param name The name of the instance.
	 * @param coreAmount The core amount of the instance.
	 * @throws HostNotFoundException Thrown when the physical host is not found.
	 */
	protected Instance(Image image, CloudTOBEMERGEWITHCOMPUTE cloud, CloudTOBEMERGEWITHCOMPUTE.VMHost vmhost, String name, int coreAmount, Mutex imageTransferMutex) {
		this(image, cloud, vmhost, name, coreAmount, imageTransferMutex, 0);
	}
	
	
	/**
	 * Constructor to start now.
	 * @TODO Mime VM
	 * @param image The image of the instance.
	 * @param cloud The cloud of the instance.
	 * @param host The physical host of the instance. 
	 * @param name The name of the instance.
	 * @param coreAmount The core amount of the instance.
	 * @param delay The delay (in s) to wait before starting.
	 * @throws HostNotFoundException Thrown when the physical host is not found.
	 */
	protected Instance(Image image, CloudTOBEMERGEWITHCOMPUTE cloud, CloudTOBEMERGEWITHCOMPUTE.VMHost vmhost, String name, int coreAmount, Mutex imageTransferMutex, int delay) {
		super(vmhost.host, name);
		
		this.cloud  = cloud;
		this.vmhost = vmhost;
		this.image  = image;
		
		this.runningMutex = new Mutex();
		this.imageTransferMutex = imageTransferMutex;
		
		stateLog = new Vector<StateDate>();
		
		try {
			computeNodeProcess = new ComputeNodeProcess(this);
		} catch (HostNotFoundException e) {
			Msg.critical("Something bad happened in SimIaaS: The host "+this+"was not found" );
			e.printStackTrace();
		}
		
		cloud.instancesCount++;
		vmhost.instancesCount++;
		vmhost.bootingInstancesCount++;
		
		//Trace.hostPushState (this.getHost().getName(), "VM_STATE", "PENDING");
		setState(STATE.PENDING);
	}
	
	/**
	 * @return The physical host of the instance.
	 */
	public Host getHost() {
		return vmhost.host;
	}
	
	/**
	 * Sets the state of the instance.
	 * @param state the state of the instance, from now on.
	 */
	protected void setState(STATE state)
	{
		this.state=state;
		stateLog.add(new StateDate(state,Msg.getClock()));
		
		//Trace.vmVariableSet(state.toString(),getName(),Msg.getClock());
		//Trace.hostPopState (this.getHost().getName(), "VM_STATE");
 		//Trace.hostPushState (this.getHost().getName(), "VM_STATE", state.name());
		
		Msg.verb("State of "+name+" changed to "+state);
	}
	
	/**
	 * Gets the current state of the instance.
	 * @return the current state of the instance.
	 */
	public STATE getState()
	{
		return state;
	}
	
	/**
	 * Get the date of the last change to a given state.
	 * @param state The state you are looking for.
	 * @return The date you are looking for.
	 */
	public double getDateOfLast(STATE state)
	{
		for (int i=stateLog.size()-1; i>=0; i--)
			if (stateLog.get(i).state == state)
				return stateLog.get(i).date;
		return -1;
	}
	
	/**
	 * Get the date of the first change to a given state.
	 * @param state The state you are looking for.
	 * @return The date you are looking for.
	 */
	public double getDateOfFirst(STATE state)
	{
		for (int i=0; i<stateLog.size(); i++)
			if (stateLog.get(i).state == state)
				return stateLog.get(i).date;
		return -1;
	}
	
		
	
	/**
	 * Wait until this instance is running.
	 * @throws HostFailureException Thrown when the controller host or the vm fails.
	 */
	public void waitForRunning() throws HostFailureException
	{
		runningMutex.acquire();
		runningMutex.release();
		
		while (state != STATE.RUNNING) {
			Process.currentProcess().waitFor(1);
			runningMutex.acquire();
			runningMutex.release();
		}
	}
	
	
	/**
	 * Trigger a new command for this instance.
	 * @param command The command to be triggered.
	 * @return the ControlProcess of this command.
	 */
	protected void doCommand(ComputeControllerProcess.COMMAND command)
	{
		Msg.verb("Command "+command+" on "+ getName()+ " issued");
		cloud.computeControllerProcess.enqueueControlTask(new ControlTask(this,command));
	}
	
	/**
	 * Called by the ControlProcess running on the Controller before executing a command.
	 * Set the state and call relevant VM methods.
	 * @param command The type of command.
	 */
	protected void commandInit(ComputeControllerProcess.COMMAND command)
	{
		switch (command) {

		case BOOT :	
			runningMutex.acquire();
			break;
		
		case SUSPEND :
			runningMutex.acquire();
			setState(STATE.SUSPENDING);
			break;
		
		case RESUME :
				/*
			for (Process p : processes) {
				Msg.info("resuming " + p.toString());
				p.resume();
			}
			*/
			setState(STATE.RESUMING);
			break;
			
		case REBOOT :
			runningMutex.acquire();
			super.suspend();
			computeNodeProcess.resume();
			
			setState(STATE.REBOOTING);
			break;
			
		case SHUTDOWN :
			setState(STATE.SHUTTINGDOWN);
			break;
		}
	}
	
	/**
	 * Called by the ControlProcess running on the Controller at the completion of a command.
	 * Set the state and call relevant VM methods.
	 * @param command The type of command.
	 */
	protected void commandFinalize(ComputeControllerProcess.COMMAND command)
	{
		switch (command) {
		
		case BOOT :
			vmhost.bootingInstancesCount--;
			//super.resume();
			start();
			setState(STATE.RUNNING);
			runningMutex.release();			
			break;
		
		case SUSPEND :
			super.suspend();
			computeNodeProcess.resume();
			setState(STATE.SUSPENDED);
			break;
		
		case RESUME :
			super.resume();
			setState(STATE.RUNNING);
			runningMutex.release();			
			break;
			
		case REBOOT :		
			setState(STATE.RUNNING);
			runningMutex.release();			
			break;
			
		case SHUTDOWN :
			setState(STATE.TERMINATED);
			//Trace.hostPopState (this.getHost().getName(), "VM_STATE");
			
			cloud.instancesCount--;
			vmhost.instancesCount--;
			
			super.shutdown();
			super.destroy();
			break;
		}
	}
	
		
	
	/**
	 * Suspends the instance.
	 * Starts a command in order to, then suspends the VM.
	 * @see org.simgrid.msg.VM#suspend()
	 */
	public void suspend() {
		doCommand(ComputeControllerProcess.COMMAND.SUSPEND);
	}
	
	/**
	 * Resumes the instance.
	 * Starts a command in order to, then resumes the VM.
	 * @see org.simgrid.msg.VM#resume()
	 */
	public void resume() {
		doCommand(ComputeControllerProcess.COMMAND.RESUME);
	}
		
	/**
	 * Terminates the instance.
	 * Starts a command in order to, then shutdown the VM.
	 * @see org.simgrid.msg.VM#shutdown()
	 */
	public void terminate() {
		doCommand(ComputeControllerProcess.COMMAND.SHUTDOWN);
	}
	
	/**
	 * Reboot the instance.
	 * Starts a command in order to, then shutdown the VM and boot it.
	 * @see org.simgrid.msg.VM#start()
	 */
	public void reboot() {
		doCommand(ComputeControllerProcess.COMMAND.REBOOT);
	}
	
 	
	/**
	 * Used to print the whole instance life-cycle.
	 * @return A string containing all the events of this instance.
	 */
	public String stateLogToString(){
		String log = "Log of "+getName();
		for (StateDate sd : stateLog) {
			log+="\n"+sd;
		}
		return log;
	}
	
	/**
	 * Used to print the instance life-cycle in short (only the date of pending, running, shuttingdown, and terminated).
	 * @return A string containing the main events of this instance.
	 */	public String toString()
	{
		return "Instance : "+getName()+" : "+vmhost.host.getName()+" : "+getDateOfFirst(STATE.PENDING)+" : "+getDateOfFirst(STATE.RUNNING)+" : "+getDateOfLast(STATE.SHUTTINGDOWN)+" : "+getDateOfLast(STATE.TERMINATED);
	}

	/**
	 * Gets the message box used to control this instance.
	 * @return The message box used to control this instance.
	 */
	protected String getControlMessageBox() {
		return "ControlCommand:"+getName();
	}

	public double getSpeed() {
		return vmhost.host.getSpeed();
	}
	
}
