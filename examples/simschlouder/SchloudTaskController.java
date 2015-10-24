package simschlouder;


import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;

/**
 * Represents a controller of SchloudTasks
 * @author julien.gossa@unistra.fr
 */
public class SchloudTaskController extends Process {
	
	/** the node running this controller */
	protected SchloudNode node;

	/**
	 * Constructor
	 * @param node the node to run this controller.
	 */
	protected SchloudTaskController(SchloudNode node) {
		super(SchloudController.host, "SchloudTaskController:"+node.instance);
		
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
			
			schloudTask.getCommandTask().send(node.getMessageBox());
			
			schloudTask.setState(SchloudTask.STATE.SUBMITTED);			
			node.setState(SchloudNode.STATE.BUSY);
			
			//Msg.info("waiting for complete " + schloudTask.name);
			Task rt = Task.receive(node.getMessageBox());
			//Msg.info("complete received " + schloudTask.name);
			try {
				rt.execute();
			} catch (TaskCancelledException e) {
			}
			
			node.completedQueue.add(node.queue.poll());
			
			schloudTask.setState(SchloudTask.STATE.COMPLETE);
			// correct the idleDate
			node.idleDate+=schloudTask.getWalltime()-schloudTask.getWalltimePrediction();
		}
		
		node.setState(SchloudNode.STATE.IDLE);

		//Msg.info("SchloudTaskController "+name+" finished");
		//node.handleNextTask();
	}
}
