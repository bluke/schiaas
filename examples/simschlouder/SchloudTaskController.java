package simschlouder;


import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;

import simschlouder.SchloudNode.STATE;


public class SchloudTaskController extends Process {
	
	protected SchloudNode node;

	protected SchloudTaskController(SchloudNode node) {
		super(SchloudController.host, "SchloudTaskProcess:"+node.instanceId);
		
		this.node = node;
	}

	public void main(String[] arg0) throws MsgException {
		
		Msg.verb("SchloudTaskController "+name+" waiting for its node "+node.id+ " to boot");
		//TODO optimize wait time according to boot/delay times
		while (node.state == STATE.PENDING ) {
			Msg.verb("wait for boot "+ node.state);
			waitFor(10);
		}
				
		while (!node.queue.isEmpty()) {
		
			node.setState(SchloudNode.STATE.BUSY);
			
			SchloudTask schloudTask=node.queue.peek();
			node.currentSchloudTask = schloudTask;
			
			schloudTask.setState(SchloudTask.STATE.SUBMITTED);
			
			schloudTask.getCommandTask().send(node.getMessageBox());
			
			//Msg.info("waiting for complete " + task.name);
			Task rt = Task.receive(node.getMessageBox());
			//Msg.info("complete received " + task.name);
			try {
				rt.execute();
			} catch (TaskCancelledException e) {
			}
			
			node.completedQueue.add(node.queue.poll());
			
			schloudTask.setState(SchloudTask.STATE.COMPLETE);
			// correct the idleDate
			//node.idleDate+=stask.getRuntime()-stask.predictedRuntime;
			node.idleDate+=schloudTask.getWalltime()-schloudTask.getWalltimePrediction();
		}		
		 
		node.setState(SchloudNode.STATE.IDLE);

		//Msg.info("SchloudTaskController "+name+" finished");
		//node.handleNextTask();
	}
}
