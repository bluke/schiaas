package simschlouder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;


public class SchloudTask {
	
	public enum STATE {PENDING,SUBMITTED,RUNNING,FINISHED,COMPLETE};
	
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
	protected double dataIn;
	protected double dataOut;
	protected double duration;
	
	protected double runtimePrediction = 0;
	protected double walltimePrediction = 0;
	
	protected STATE state;
	
	/**
	 * The log of the state changes.
	 */
	protected Vector<StateDate> stateLog;
	
	protected Vector<SchloudTask> dependencies;
	
	protected static final double defaultDataSize = 1000;
	
	public SchloudTask(String name, double runtimePrediction, double dataIn,  double dataOut) {
		this.name = name;
		this.dataIn = dataIn;
		this.duration = SimSchlouder.time2flops(runtimePrediction);
		this.dataOut = dataOut;
		
		this.runtimePrediction=runtimePrediction;
		this.walltimePrediction=runtimePrediction;
		
		dependencies = new Vector<SchloudTask>();
		
		stateLog = new Vector<StateDate>();
		setState(STATE.PENDING);
	}
		
	public SchloudTask(String name, double givenDuration, double input) {
		this (name, givenDuration, input, defaultDataSize);
	}
	
	public SchloudTask(String name, double givenDuration) {
		this (name, givenDuration, defaultDataSize, defaultDataSize);
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
	
	public void addDependency(SchloudTask task) {
		dependencies.add(task);
	}
	
	public boolean hasPendingDependencies(){
		for (SchloudTask t : dependencies) {
			if (t.state != SchloudTask.STATE.COMPLETE)
				return true;
		}
		return false;
	}
	
	public Task getInputTask() {
		return new Task(name+"_input", duration, dataIn);
	}
	
	public Task getOutputTask() {
		return new Task(name+"_output", 0, dataOut);
	}

	public String toString() {
		return "jid: "+name+"\t"+ getDateOfFirst(STATE.PENDING) + "\t" + getDateOfFirst(STATE.RUNNING) + "\t" + getDateOfFirst(STATE.COMPLETE);
	}

	public double getRuntime() {
		return getDateOfLast(STATE.COMPLETE) - getDateOfFirst(STATE.RUNNING);
	}

	
	// TODO: add scheduling_strategy
	// TODO: check walltime/runtime
	public void writeJSON(BufferedWriter out) throws IOException {
		out.write("\t\t\t\t\"id\": \""+name+"\",\n");
		out.write("\t\t\t\t\"submission_date\": "+getDateOfFirst(STATE.PENDING)+",\n");
		out.write("\t\t\t\t\"start_date\": "+getDateOfFirst(STATE.RUNNING)+",\n");
		out.write("\t\t\t\t\"walltime\": "+getRuntime()+",\n"); // TERMINATED AND NOT SHUTINGDOWN
		out.write("\t\t\t\t\"walltime_prediction\": "+runtimePrediction+"\n");
		out.write("\t\t\t\t\"provisioning_strategy\": \""+SchloudController.strategy.getName()+"\",\n"); // NOT THE PREDICTION USED ACTUALLY
		out.write("\t\t\t\t\"dependencies\": [\n");
		for (SchloudTask schloudTask : dependencies) {
			out.write("\t\t\t\t\t\""+schloudTask.name+"\",\n");
		}
		out.write("\t\t\t\t],\n");
		out.write("\t\t\t\t\"PSM_data\": {\n");
		out.write("\t\t\t\t\t\"runtime_prediction\": \""+runtimePrediction+"\",\n");
		out.write("\t\t\t\t\t\"data_in\": \""+dataIn+"\",\n");
		out.write("\t\t\t\t\t\"data_out\": \""+dataOut+"\",\n");
		out.write("\t\t\t\t},\n");
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
	
	public Vector<SchloudTask> getDependencies() {
		return new Vector<SchloudTask>(this.dependencies);
	}
	
	public double getInputSize() {
		return this.dataIn;
	}
	
	public double getOutputSize() {
		return this.dataOut;
	}
	
	public double getDuration() {
		return this.duration;
	}
	
	public double getPredictedDuration() {
		return this.runtimePrediction;
	}
	
	// Does not take into consideration transfer times
	protected double getRuntimePredictionOn(SchloudNode node) {
		//predictedRuntime = computeDuration / node.instance.getSpeed();
		return duration / node.getSpeed();
	}
}
