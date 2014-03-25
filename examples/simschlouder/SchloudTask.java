package simschlouder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Data;


public class SchloudTask {
	
	public enum STATE {PENDING,SUBMITTED,INPUTTING,RUNNING,OUTPUTTING,FINISHED,COMPLETE};
	
	/**
	 * Represents an event for the instance: a date and the state of the instance since this date.
	 * @author julien
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

	
	protected String name;
	
	protected double walltimePrediction;
	
	protected double duration;
	protected double inputSize;
	protected double outputSize;
	
	protected double PSMRuntime;
	protected double PSMDataIn;
	protected double PSMDataOut;
	
	protected STATE state;
	
	/**
	 * The log of the state changes.
	 */
	protected Vector<StateDate> stateLog;
	
	protected Vector<SchloudTask> dependencies;
	
	protected static final double commandDataSize = 1000;
	protected static final double commandDuration = 1000;
	
	
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
	
	
	public SchloudTask(String name, double runtime, double inputSize,  double outputSize, Vector<SchloudTask> dependencies) {
		this (name, runtime, runtime, inputSize, outputSize, new Vector<SchloudTask>());
	}

	public SchloudTask(String name, double runtime, double inputSize, double outputSize) {
		this (name, runtime, inputSize, outputSize, new Vector<SchloudTask>());
	}

	
	public SchloudTask(String name, double runtime, double inputSize) {
		this (name, runtime, inputSize, 0);
	}
	
	public SchloudTask(String name, double runtime) {
		this (name, runtime, 0);
	}
		
	/**
	 * Get the date of the last change to a given state.
	 * @param state The state you are looking for.
	 * @return The date you are looking for.
	 */
	public double getDateOfLast(STATE state) {
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
	public double getDateOfFirst(STATE state) {
		for (int i=0; i<stateLog.size(); i++)
			if (stateLog.get(i).state == state)
				return stateLog.get(i).date;
		return -1;
	}
		
	public boolean hasPendingDependencies(){
		if (dependencies!=null)
			for (SchloudTask t : dependencies) {
				if (t.state != SchloudTask.STATE.COMPLETE)
					return true;
			}
		return false;
	}
	
	public Task getCommandTask() {
		return new Task(name+"_command", commandDuration, commandDataSize);
	}

	public Task getCompleteTask() {
		return new Task(name+"_complete", commandDuration, commandDataSize);
	}
	
	
	public Task getInputTask() {
		return new Task(name+"_input", 0, inputSize);
	}

	public Data getInputData() {
		return new Data(name+"_input_data", inputSize);
	}
	
	public Task getRunTask() {
		return new Task(name+"_run", duration, 0);
	}
	
	public Task getOutputTask() {
		return new Task(name+"_output", 0, outputSize);
	}

	public Data getOutputData() {
		return new Data(name+"_output_data", outputSize);
	}
	
	// TODO: add scheduling_strategy
	// TODO: check walltime/runtime
	public void writeJSON(BufferedWriter out) throws IOException {
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
	
	public STATE getState(){
		return state;
	}
		
	public String getName() {
		return this.name;
	}
	
	public void addDependency(SchloudTask schloudTask) {
		this.dependencies.add(schloudTask);
	}
	
	public Vector<SchloudTask> getDependencies() {
		return new Vector<SchloudTask>(this.dependencies);
	}
	
	public double getWalltimePrediction() {
		return this.walltimePrediction;
	}
	
	public double getInputSize() {
		return this.inputSize;
	}
	
	public double getOutputSize() {
		return this.outputSize;
	}
	
	public double getDuration() {
		return this.duration;
	}

	public double getRuntime() {
		return getDateOfFirst(STATE.OUTPUTTING) - getDateOfLast(STATE.RUNNING);
	}
	
	private double getOutputTime() {
		return getDateOfLast(STATE.FINISHED) - getDateOfFirst(STATE.OUTPUTTING);
	}

	private double getInputTime() {
		return getDateOfFirst(STATE.RUNNING) - getDateOfFirst(STATE.INPUTTING);
	}

	private double getWallTimePrediction() {
		return walltimePrediction;
	}

	private double getWalltime() {
		return getDateOfFirst(STATE.COMPLETE) - getDateOfFirst(STATE.SUBMITTED);
	}

	private double getStartDate() {
		return getDateOfFirst(STATE.INPUTTING);
	}

	private double getSubmissionDate() {
		return getDateOfFirst(STATE.PENDING);
	}
	
	private double getManagementTime() {
		return getWalltime()-getInputTime()-getRuntime()-getOutputTime();
	}
	
	
	
	public String toString() {
		return "Task["+name+","+walltimePrediction+","+duration+"("+PSMRuntime+" s),"+inputSize+","+outputSize+","+state+"]";
	}
		
}
