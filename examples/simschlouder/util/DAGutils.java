package simschlouder.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.simgrid.msg.Msg;

import simschlouder.SchloudTask;

/**
 * Handles DAG specific operations such as listing the successors and predecessors
 * of a given task, adding a new task to the DAG or computing priorities based
 * on rank or level.
 * @author mfrincu
 *
 */
public final class DAGutils {
	
	private LinkedList<SchloudTask> tasks = null;	
	private static DAGutils dag = null; 
	private TreeMap<String, Vector<SchloudTask>> successors = null;
		
	/**
	 * Default constructor. This method is private since we use a singleton pattern on this class since
	 * we only use it to find out information on the submitted DAG
	 * @param tasks the list of tasks making up the DAG
	 * @throws SimSchlouderException 
	 */
	private DAGutils(LinkedList<SchloudTask> tasks) throws SimSchlouderException {
		if (tasks == null)
			throw new SimSchlouderException("Task list cannot be null");
		this.tasks = tasks;
		this.computeAllSuccessors();
	}
	
	/**
	 * Default constructor. It does not populate the task list. The user will have to use
	 * the exposed methods do do that afterwards.
	 * @throws SimSchlouderException
	 */
	private DAGutils() throws SimSchlouderException {		
		this.tasks = new LinkedList<SchloudTask>();
	} 
	
	/**
	 * Reference to the <i>DAGutils</i> object
	 * @param tasks the list of tasks making up the DAG
	 * @return the reference to the object
	 * @throws SimSchlouderException 
	 */
	public static DAGutils getDAG(LinkedList<SchloudTask> tasks) throws SimSchlouderException {
		if (DAGutils.dag == null) {
			DAGutils.dag = new DAGutils(tasks);
		}
		return DAGutils.dag;
	}
	
	/**
	 * Reference to the <i>DAGutils</i> object. It returns an object already populated with tasks
	 * or a one that without them.
	 * @return the reference to the object
	 * @throws SimSchlouderException
	 */
	public static DAGutils getDAG() throws SimSchlouderException {
		if (DAGutils.dag == null) {
			DAGutils.dag = new DAGutils();
		}
		return DAGutils.dag;
	}
	
	/**
	 * Add a list of tasks to the already existing ones. Useful if workflow tasks arrive in batches 
	 * @param tasks the list of new tasks
	 */
	public void addTasks(LinkedList<SchloudTask> tasks) {
		this.tasks.addAll(tasks);
		this.computeAllSuccessors();
	}
	
	/**
	 * Adds a new workflow task to the already existing ones 
	 * @param task the new task
	 */
	public void addTask(SchloudTask task) {
		this.tasks.add(task);
		this.computeAllSuccessors();
	}
	
	public LinkedList<SchloudTask> getLevelTasks(SchloudTask task) {
		TreeMap<Integer, LinkedList<SchloudTask>> levels = getTasksPerLevel();
		LinkedList<SchloudTask> levelTasks = null;
		for (Integer l : levels.keySet()) {
			levelTasks = levels.get(l);
			if (levelTasks.contains(task)) {
				return levelTasks;
			}
		}
		return null;
	}
	
	/**
	 * Orders tasks descending based on their ranks. Tasks can then be executed
	 * following this ordering
	 * @return the sorted list of tasks or null in case of error
	 */
	public LinkedList<SchloudTask> computePriorityAndRank() {
		LinkedList<SchloudTask> sorted = new LinkedList<SchloudTask>();
		
		Map<String,Double> priorities = new TreeMap<String, Double>(); 
		for (SchloudTask t : this.tasks) {
			try {
				priorities.put(t.getName(), this.computePriority(t));
			} catch (SimSchlouderException e) {
				Msg.error(e.getMessage());
				return null;			
			}
		}
		
		for (Map.Entry<String, Double> entry : entriesSortedByValues(priorities)) {
			for (SchloudTask t : this.tasks) {
				if (t.getName().compareToIgnoreCase(entry.getKey())==0) {
					sorted.add(t);
				}
			}
		}
		
		return sorted;
	}
	
	/**
	 * Computes the levels of parallelism in the DAG
	 * @return an ordered <i>TreeMap</i> in which the keys represents one level  or null in case of error
	 */
	public TreeMap<Integer, LinkedList<SchloudTask>> getTasksPerLevel() {
		
		TreeMap<Integer, LinkedList<SchloudTask>> levels = new TreeMap<Integer, LinkedList<SchloudTask>>(); 
		LinkedList<SchloudTask> l = null;
		int level = 0;
		
		for (SchloudTask t : this.tasks) {
			try {
				level = this.getLevel(t, 0);
			} catch (SimSchlouderException e) {
				Msg.error(e.getMessage());
				return null;
			}
			l = levels.get(level);
			if (l == null) {
				l = new LinkedList<SchloudTask>();
			}
			l.add(t);
			levels.put(level,l);
		}
		
		return levels;
	}
	
	/**
	 * Computes the predecessors of a given task
	 * @param t the task
	 * @return a list of predecessors
	 * @throws SimSchlouderException 
	 */
	public Vector<SchloudTask> predecessor(SchloudTask t) throws SimSchlouderException {
		if (t == null)
			throw new SimSchlouderException("Task for which predecessor is computed cannot be null");
		
		return t.getDependencies();
	}
	
	/**
	 * Computes the successors of a task
	 * @param t the task
	 * @return a list of successors
	 * @throws SimSchlouderException 
	 */
	public Vector<SchloudTask> succesor(SchloudTask t) throws SimSchlouderException {		
		if (t == null)
			throw new SimSchlouderException("Task for which the successor is computed cannot be null");
		
		return this.successors.get(t.getName());
		
	}
	
	/**
	 * Method to pre-compute the predecessors for all tasks. 
	 * Avoids the overhead of doing this for large DAGs during the simulation
	 * @return an ordered <i>Map</i> containing the list of successors for each task  
	 */
	private void computeAllSuccessors() {		
		this.successors = new TreeMap<String, Vector<SchloudTask>>();
		Vector<SchloudTask> p = null;
		
		for (SchloudTask task : this.tasks) {
			p = new Vector<SchloudTask>();
			for (SchloudTask task2 : this.tasks) {
				if (task.getName().compareToIgnoreCase(task2.getName()) !=0 && task2.getDependencies().contains(task))
					p.add(task2);
			}
			this.successors.put(task.getName(), p);
		}			
	}
	
	/**
	 * Method used to recursively compute the priority of a task
	 * @param t the task
	 * @return the priority of the task
	 * @throws SimSchlouderException 
	 */
	private Double computePriority(SchloudTask t) throws SimSchlouderException {
		Vector<SchloudTask> succ = this.succesor(t);
		if (succ.size() > 0) {
			Vector<Double> vals = new Vector<Double>(succ.size());
			for (SchloudTask s : succ) {
				vals.add(t.getOutputSize() + s.getInputSize() +computePriority(s));
			}
			return t.getDuration() + Collections.max(vals);
		}
		else {
			return t.getDuration();
		}
	}
	
	/**
	 * Method used to recursively compute the level of a task
	 * @param t the task
	 * @param level the current level
	 * @return the level
	 * @throws SimSchlouderException 
	 */
	private int getLevel(SchloudTask t, int level) throws SimSchlouderException {
		Vector<SchloudTask> preds = this.predecessor(t);
		if (preds.size() == 0) {
			return level;
		}
		else {
			int m = 0, crt = 0;
			for (SchloudTask p : preds) {
				crt = this.getLevel(p, level+1);
				if (crt > m) {
					m = crt;
				}
			}
			return m;
		}
	}
	
	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                    int res = e2.getValue().compareTo(e1.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
	
	/**
	 * Test method
	 * @param args
	 * @throws SimSchlouderException 
	 */
	public static void main(String[] args) throws SimSchlouderException {
		LinkedList<SchloudTask> tasks = new LinkedList<SchloudTask>();
		
		SchloudTask task = new SchloudTask("1",200,10,20);
		tasks.add(task);
		
		SchloudTask task2 = new SchloudTask("2",300,20,10);
		task2.addDependency(task);
		tasks.add(task2);
		
		SchloudTask task3 = new SchloudTask("3",300,20,11);
		task3.addDependency(task);
		tasks.add(task3);
		
		SchloudTask task4 = new SchloudTask("4",100,10,40);
		task4.addDependency(task2);
		task4.addDependency(task3);
		tasks.add(task4);
		
		System.out.println("Rank ordering:");
		DAGutils dag = DAGutils.getDAG(tasks);
		LinkedList<SchloudTask> rank = dag.computePriorityAndRank();
		for (SchloudTask t : rank) {
			System.out.println(t.getName());
		}
		
		SchloudTask task5 = new SchloudTask("5",100,10,40);
		task5.addDependency(task4);			
		
		dag.addTask(task5);
		
		System.out.println("Levels:");
		TreeMap<Integer, LinkedList<SchloudTask>> levels = dag.getTasksPerLevel();
		for (Integer l : levels.keySet()) {
			System.out.println(l + " " + levels.get(l));
		}
		
		System.out.println("Find level tasks for task: " + task2.getName());
		System.out.println(dag.getLevelTasks(task2));
	}

}