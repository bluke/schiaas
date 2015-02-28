package simschlouder.algorithms;

import java.util.LinkedList;
import java.util.TreeMap;

import org.simgrid.msg.Msg;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;
import simschlouder.util.DAGutils;
import simschlouder.util.SimSchlouderException;

/**
 * Handles the task placement. Relies on scheduling and allocation policies
 * specific to each algorithm.
 * This class is to be used as base class by any scheduling/provisioning method.
 * @author mfrincu
 * TODO find a better way than to use static fields from SchloudController to access task and node list 
 */
public abstract class AStrategy {
	
	/**
	 * Ordering options for workflow tasks:
	 * NONE: no explicit ordering. Each task is executed as soon as its parents have completed their execution
	 * PRIORITY_RANKING: ranking based on on priorities given by task execution and transfer times.
	 * Tasks are ordered descending based on the computed priorities. 
	 * LEVEL_RANKING: ranking based on the task level parallelism. Initial tasks are scheduled first.
	 * @author mfrincu
	 *
	 */
	protected enum ORDERING {NONE, PRIORITY_RANKING, LEVEL_RANKING};
	
	/**
	 * Runs the task placement strategy
	 * @throws SimSchlouderException
	 */
	public void execute() throws SimSchlouderException {		
		SchloudController.schloudCloud.resetBootCount();
		
		TreeMap<Integer, LinkedList<SchloudTask>> tasks = null;
		DAGutils dag = DAGutils.getDAG(SchloudController.mainQueue);
		
		//decides whether or not to use ordering.
		//this is usually determined by the type of tasks: bag-of-tasks or workflows
		switch (this.getOrdering()) {
			case NONE:
				tasks = new TreeMap<Integer, LinkedList<SchloudTask>>();
				tasks.put(1, SchloudController.mainQueue);
				break;
			case PRIORITY_RANKING:
				tasks = new TreeMap<Integer, LinkedList<SchloudTask>>();
				tasks.put(1, dag.computePriorityAndRank());
				break;
			case LEVEL_RANKING:
				tasks = dag.getTasksPerLevel();
				break;
			default:
				break;
			
		}
		
		//cycle through all tasks and assign them based on the strategy
		for (Integer l : tasks.keySet()) { 
			int iTask = 0;
			while (iTask<tasks.get(l).size()) {
				SchloudTask task = tasks.get(l).get(iTask);
				SchloudNode node = null;
				if (task.hasPendingDependencies()) {
					iTask++;
					continue;
				}

				//apply strategy
				node = this.applyStrategy(task);
				
				if (node == null && SchloudController.schloudCloud.describeAvailability(SchloudController.instanceTypeId)>0 ) {
					node = SchloudController.startNewNode();
				}
				
				if (node != null) {
					Msg.info(task.getName() + " selected node: " + node.instanceId);
					SchloudController.setTaskToNode(tasks.get(l).remove(iTask), node);
					SchloudController.mainQueue.remove(task);
				} else {
					iTask++;
				}
			}
		}
		//System.out.println("Tasks on main queue: " + SchloudController.mainQueue.size());
	}
	
	/**
	 * Retrieves the name of the method
	 * @return the algorithm's name
	 */
	public abstract String getName();
	
	/**
	 * Applies the algorithm specific heuristics. 
	 * Return null if you want to allocate a new VM for the task
	 * @param task the task to be scheduled
	 * @return one of the existing nodes or null in case none is valid
	 */
	protected abstract SchloudNode applyStrategy(SchloudTask task);
	
	/**
	 * Retrieves the ordering method. It is one of: NONE, PRIORITY_RANKING, LEVEL_RANKING
	 * @return the ordering method
	 */
	protected abstract ORDERING getOrdering();

}

