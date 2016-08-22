package simschlouder;


import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;

import simschlouder.SchloudTask.STATE;
import simschlouder.util.SimSchlouderException;

/**
 * Represents a controller of SchloudTasks
 * @author julien.gossa@unistra.fr
 */
public class SchloudTaskController extends Process {
	
	/** the node running this controller */
	protected SchloudNode node;

	/**
	 * Constructor
	 * @param host the host to run this controller.
	 */
	protected SchloudTaskController(SchloudNode node) {
		super(SchloudController.controller, "SchloudTaskController:"+node.instance);
		
		this.node = node;
	}

	/**
	 * Execute SchloudTasks until there is no more.
	 */
	public void main(String[] arg0) throws MsgException {
		/*
		Msg.verb("SchloudTaskController "+name+" waiting for its node "+node.id+ " to boot");
		//TODO optimize wait time according to boot/delay times
		while (node.state == STATE.PENDING ) {
			Msg.verb("wait for boot "+ node.state);
			waitFor(10);
		}
			*/	
		while (!node.queue.isEmpty()) {
			
			SchloudTask schloudTask=node.queue.peek();
			node.currentSchloudTask = schloudTask;
			
			schloudTask.setState(SchloudTask.STATE.SUBMITTED);
			node.setState(SchloudNode.STATE.BUSY);
			
			schloudTask.getCommandTask().send(node.getMessageBox());
			
			//Msg.info("waiting for complete " + schloudTask.name);
			Task rt = Task.receive(node.getMessageBox());
			//Msg.info("complete received " + schloudTask.name);
			try {
				rt.execute();
			} catch (TaskCancelledException e) {
			}
			
			// handle the management time
			if (schloudTask.managementTime != 0) {
				double adjustment = 0;
				if (schloudTask.inputSize==0 && schloudTask.outputSize==0) {
					try {
						adjustment = schloudTask.managementTime 
								- (schloudTask.getDateOfFirst(STATE.INPUTTING) - schloudTask.getDateOfFirst(STATE.SUBMITTED))
								- (Msg.getClock() - schloudTask.getDateOfFirst(STATE.FINISHED));
					} catch (SimSchlouderException e) {
						Msg.warn("Something went wrong while computing the adjustement for management time.");
						e.printStackTrace();
					}
					
					if (adjustment<0) {
						Msg.warn("Can't go back in time for the adjustement of the management time of "
								+schloudTask.name+": "+adjustment
								+" ("+(schloudTask.managementTime-adjustment)+" instead of "+ schloudTask.managementTime);
					} else {
						Msg.verb("Adjusting the management time of "+schloudTask.name+": "+adjustment);
						waitFor(adjustment);
					}
				}
			}
			
			node.completedQueue.add(node.queue.poll());
			schloudTask.setState(SchloudTask.STATE.COMPLETE);
			// correct the idleDate
			node.idleDate+=schloudTask.getWalltime()-schloudTask.getWalltimePrediction();
		}
		
		node.setState(SchloudNode.STATE.IDLE);
	}
}
