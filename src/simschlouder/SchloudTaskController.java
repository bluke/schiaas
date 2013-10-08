package simschlouder;


import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;


public class SchloudTaskController extends Process {
	
	protected SchloudNode node;

	protected SchloudTaskController(SchloudNode node) {
		super(SchloudController.host, "SchloudTaskProcess:"+node.instanceId);
		
		this.node = node;
	}

	public void main(String[] arg0) throws MsgException {
		
		node.setState(SchloudNode.STATE.BUSY);
		
		while (!node.queue.isEmpty()) {
			
			SchloudTask stask=node.queue.peek();
			node.currentSchloudTask = stask;
			
			stask.setState(SchloudTask.STATE.SUBMITTED);
			
			stask.getInputTask().send(node.getMessageBox());
			
			//Msg.info("waiting for complete " + task.name);
			Task rt = Task.receive(node.getMessageBox());
			//Msg.info("complete received " + task.name);
			try {
				rt.execute();
			} catch (TaskCancelledException e) {
			}
			
			node.completedQueue.add(node.queue.poll());
			
			stask.setState(SchloudTask.STATE.COMPLETE);
			// correct the idleDate
			//node.idleDate+=stask.getRuntime()-stask.predictedRuntime;
		}		
		 
		node.setState(SchloudNode.STATE.IDLE);

		//Msg.info("SchloudTaskController "+name+" finished");
		//node.handleNextTask();
	}
}
