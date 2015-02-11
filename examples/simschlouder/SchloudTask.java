package simschlouder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Data;

import simschlouder.util.SimSchlouderException;

/**
 * Represents a task in the SimSchlouder system
 * @author julien.gossa@unistra.fr
 *
 */
public class SchloudTask {

	/**
	 * Enumerates the different states of a task in SimSchlouder
	 * @author julien.gossa@unistra.fr
	 *
	 */
	public enum STATE {PENDING,SUBMITTED,INPUTTING,RUNNING,OUTPUTTING,FINISHED,COMPLETE};
	
	/**
	 * Represents an event for the instance: a date and the state of the instance since this date.
	 * @author julien.gossa@unistra.fr
	 */
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

	/** The name of the task */
	protected String name;
	
	/** The prediction of the walltime of the task */
	protected double walltimePrediction;
	
	/** MSG's duration of the task */
	protected double duration;
	/** Size of the input data of the task */
	protected double inputSize;
	/** Size of the output data of the task */
	protected double outputSize;
	
	/** Runtime, as Schlouder gave to the PSM */
	protected double PSMRuntime;
	/** size of the input data, as Schlouder gave to the PSM */
	protected double PSMDataIn;
	/** Size of the output data, as Schlouder gave to the PSM */
	protected double PSMDataOut;
	
	/** Current state of the task */
	protected STATE state;
	
	/** The log of the changes of state */
	protected Vector<StateDate> stateLog;
	
	/** Dependencies of this task */
	protected Vector<SchloudTask> dependencies;
	
	/** Size of a command message */
	protected static final double commandDataSize = 1000;
	/** MSG's duration of a command task */ 
	protected static final double commandDuration = 1000;
	
	/**
	 * Constructor with full information
	 * @param name name of this task
	 * @param walltimePrediction prediction of the walltime of this task
	 * @param runtime runtime of this task
	 * @param inputSize size of the input data
	 * @param outputSize size of the output data
	 * @param dependencies set of dependencies of this task
	 */
	public SchloudTask(String name, double walltimePrediction, double runtime, double inputSize,  double outputSize, Vector<SchloudTask> dependencies) {
		this.name = name;
		
		this.walltimePrediction = walltimePrediction;
		
		this.duration = SimSchlouder.time2flops(runtime);
		this.inputSize = inputSize;
		this.outputSize = outputSize;
		
		this.PSMRuntime = runtime;
		this.PSMDataIn = inputSize;
		this.PSMDataOut = outputSize;
		
		this.dependencies = dependencies;
		
		stateLog = new Vector<StateDate>();
		setState(STATE.PENDING);
	}
	
	/**
	 * Constructor without walltime prediction
	 * @param name name of this task
	 * @param runtime runtime of this task
	 * @param inputSize size of the input data
	 * @param outputSize size of the output data
	 * @param dependencies set of dependencies of this task
	 */
	public SchloudTask(String name, double runtime, double inputSize,  double outputSize, Vector<SchloudTask> dependencies) {
		this (name, runtime, runtime, inputSize, outputSize, new Vector<SchloudTask>());
	}

	/**
	 * Constructor without walltime prediction nor dependencies
	 * @param name name of this task
	 * @param runtime runtime of this task
	 * @param inputSize size of the input data
	 * @param outputSize size of the output data
	 */
	public SchloudTask(String name, double runtime, double inputSize, double outputSize) {
		this (name, runtime, inputSize, outputSize, new Vector<SchloudTask>());
	}

	/**
	 * Constructor without walltime prediction nor dependencies nor output data
	 * @param name name of this task
	 * @param runtime runtime of this task
	 * @param inputSize size of the input data
	 */	
	public SchloudTask(String name, double runtime, double inputSize) {
		this (name, runtime, inputSize, 0);
	}
	
	/**
	 * Constructor with only runtime
	 * @param name name of this task
	 * @param runtime runtime of this task
	 */
	public SchloudTask(String name, double runtime) {
		this (name, runtime, 0);
	}
		
	/**
	 * Get the date of the last change to a given state.
	 * @param state The state you are looking for.
	 * @return The date you are looking for.
	 */
	public double getDateOfLast(STATE state) throws SimSchlouderException {
		for (int i=stateLog.size()-1; i>=0; i--)
			if (stateLog.get(i).state == state)
				return stateLog.get(i).date;
		throw new SimSchlouderException("task " +this.name+ " never has state :" +state);
	}
	
	/**
	 * Get the date of the first change to a given state.
	 * @param state The state you are looking for.
	 * @return The date you are looking for.
	 * @throws SimSchlouderException 
	 */
	public double getDateOfFirst(STATE state) throws SimSchlouderException {
		for (int i=0; i<stateLog.size(); i++)
			if (stateLog.get(i).state == state)
				return stateLog.get(i).date;
		throw new SimSchlouderException("task " +this.name+ " never has state :" +state);
	}

	/**
	 * 
	 * @return whether this task has currently pending dependencies
	 */
	public boolean hasPendingDependencies(){
		if (dependencies!=null)
			for (SchloudTask t : dependencies) {
				if (t.state != SchloudTask.STATE.COMPLETE)
					return true;
			}
		return false;
	}
	
	/**
	 * 
	 * @return the MSG task to start this SchloudTask 
	 */
	public Task getCommandTask() {
		return new Task(name+"_command", commandDuration, commandDataSize);
	}

	/**
	 * 
	 * @return the MSG task to advertise the completion of this SchloudTask
	 */
	public Task getCompleteTask() {
		return new Task(name+"_complete", commandDuration, commandDataSize);
	}
	
	/**
	 * 
	 * @return the MSG task to transfer the input data of this SchloudTask
	 */
	public Task getInputTask() {
		return new Task(name+"_input", 0, inputSize);
	}

	/**
	 * 
	 * @return the input data
	 */
	public Data getInputData() {
		return new Data(name+"_input_data", inputSize);
	}
	
	/**
	 * 
	 * @return the MSG task to compute this SchloudTask
	 */
	public Task getRunTask() {
		return new Task(name+"_run", duration, 0);
	}
	
	/**
	 * 
	 * @return the MSG task to transfer the output of this SchloudTask
	 */
	public Task getOutputTask() {
		return new Task(name+"_output", 0, outputSize);
	}

	/**
	 * 
	 * @return the output data of this SchloudTask
	 */
	public Data getOutputData() {
		return new Data(name+"_output_data", outputSize);
	}
	
	/**
	 * Writes a JSON file for use in later processing.
	 * Unfortunately handmade, because no standard lib was found at the time.
	 * @param description the ID of the version
	 * @throws IOException
	 * @throws SimSchlouderException 
	 */
	public void writeJSON(BufferedWriter out) throws IOException, SimSchlouderException {
		// TODO: add scheduling_strategy
		// TODO: check walltime/runtime
		out.write("\t\t\t\t\"id\": \""+name+"\",\n");
		out.write("\t\t\t\t\"provisioning_strategy\": \""+SchloudController.strategy.getName()+"\",\n"); // NOT THE PREDICTION USED ACTUALLY
		out.write("\t\t\t\t\"submission_date\": "+getSubmissionDate()+",\n");
		out.write("\t\t\t\t\"start_date\": "+getStartDate()+",\n");
		out.write("\t\t\t\t\"walltime_prediction\": "+getWallTimePrediction()+",\n");
		out.write("\t\t\t\t\"walltime\": "+getWalltime()+",\n"); // TERMINATED AND NOT SHUTINGDOWN
		out.write("\t\t\t\t\"runtime\": "+getRuntime()+",\n");
		out.write("\t\t\t\t\"input_size\": "+inputSize+",\n");
		out.write("\t\t\t\t\"input_time\": "+getInputTime()+",\n");
		out.write("\t\t\t\t\"output_time\": "+getOutputTime()+",\n");
		out.write("\t\t\t\t\"output_size\": "+outputSize+",\n");
		out.write("\t\t\t\t\"management_time\": "+getManagementTime()+",\n");		
		
		out.write("\t\t\t\t\"dependencies\": [\n");
		for (int i=0; i<dependencies.size(); i++) {
			out.write("\t\t\t\t\t\""+dependencies.get(i).name+"\"");
			if (i<dependencies.size()-1) out.write(",");
			out.write("\n");	
		}
		out.write("\t\t\t\t],\n");
		
		out.write("\t\t\t\t\"PSM_data\": {\n");
		out.write("\t\t\t\t\t\"runtime_prediction\": \""+PSMRuntime+"\",\n");
		out.write("\t\t\t\t\t\"data_in\": \""+PSMDataIn+"\",\n");
		out.write("\t\t\t\t\t\"data_out\": \""+PSMDataOut+"\"\n");
		out.write("\t\t\t\t}\n");
	}

	/**
	 * Sets the state of the instance.
	 * @param state the state of the instance, from now on.
	 */
	protected void setState(STATE state)
	{
		this.state=state;
		stateLog.add(new StateDate(state,Msg.getClock()));
				
		Msg.verb("Task "+name+" state set to "+state);
	}
	
	/**
	 * 
	 * @return the current state of this task
	 */
	public STATE getState(){
		return state;
	}
	
	/**
	 * 
	 * @return the name of this task
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Add one dependency to this task
	 * @param schloudTask the task to which this task is dependent
	 */
	public void addDependency(SchloudTask schloudTask) {
		this.dependencies.add(schloudTask);
	}
	
	/**
	 * @return the set of dependencies of this task
	 */
	public Vector<SchloudTask> getDependencies() {
		return new Vector<SchloudTask>(this.dependencies);
	}
	
	/**
	 * @return the walltime prediction of this task
	 */
	public double getWalltimePrediction() {
		return this.walltimePrediction;
	}
	
	/**
	 * @return the wall time of this task
	 */
	public double getWalltime() {
		try{
		return this.getDateOfFirst(STATE.COMPLETE) - this.getDateOfFirst(STATE.SUBMITTED);
		} catch (SimSchlouderException e){
			return this.walltimePrediction;
		}
		
	}
	
	/**
	 * @return the size of the input data of this task
	 */
	public double getInputSize() {
		return this.inputSize;
	}
	
	/**
	 * @return the size of the output data of this task
	 */
	public double getOutputSize() {
		return this.outputSize;
	}
	
	/**
	 * @return the MSG duration of this task
	 */
	public double getDuration() {
		return this.duration;
	}

	/**
	 * @return the runtime of this task
	 * @throws SimSchlouderException whenever this time can not be retrieved
	 */
	public double getRuntime() throws SimSchlouderException {
		return getDateOfFirst(STATE.OUTPUTTING) - getDateOfLast(STATE.RUNNING);
	}

	/**
	 * @return the output time of this task
	 * @throws SimSchlouderException whenever this time can not be retrieved
	 */
	private double getOutputTime() throws SimSchlouderException {
		return getDateOfLast(STATE.FINISHED) - getDateOfFirst(STATE.OUTPUTTING);
	}

	/**
	 * @return the input time of this task
	 * @throws SimSchlouderException whenever this time can not be retrieved
	 */
	private double getInputTime() throws SimSchlouderException {
		return getDateOfFirst(STATE.RUNNING) - getDateOfFirst(STATE.INPUTTING);
	}

	/**
	 * @return the walltime prediction of this task
	 */
	private double getWallTimePrediction() {
		return walltimePrediction;
	}

	/*private double getWalltime() {
		return getDateOfFirst(STATE.COMPLETE) - getDateOfFirst(STATE.SUBMITTED);
	}*/

	/**
	 * @return the start date of this task
	 * @throws SimSchlouderException whenever this time can not be retrieved
	 */
	private double getStartDate() throws SimSchlouderException {
		return getDateOfFirst(STATE.INPUTTING);
	}

	/**
	 * @return the submission date of this task
	 * @throws SimSchlouderException whenever this time can not be retrieved
	 */
	private double getSubmissionDate() throws SimSchlouderException {
		return getDateOfFirst(STATE.PENDING);
	}
	
	/**
	 * @return the management time of this task (everything that is not input, output, nor computation)  
	 * @throws SimSchlouderException whenever this time can not be retrieved
	 */
	private double getManagementTime() throws SimSchlouderException {
		return getWalltime()-getInputTime()-getRuntime()-getOutputTime();
	}
	
	/**
	 * @return the ToString
	 */	
	public String toString() {
		return "Task["+name+","+walltimePrediction+","+duration+"("+PSMRuntime+" s),"+inputSize+","+outputSize+","+state+"]";
	}
		
}
