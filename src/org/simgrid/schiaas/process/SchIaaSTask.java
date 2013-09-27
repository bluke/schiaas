package org.simgrid.schiaas.process;

import org.simgrid.msg.Task;

/**
 * Basic SchIaaS task. When the inbuilt billing system is needed every task
 * created using SchIaaS should extend this class.
 * 
 * @author mfrincu
 * 
 */
public class SchIaaSTask extends Task {

	/**
	 * REQUEST - a request to a datastore (like Amazon's S3 PUT, DELETE, ...)
	 * JOB - a regular user task to be executed (instance boot, job)
	 * DATA - a data transmission task (just data no execution. Do we need it or converge with JOB?)
	 * TODO add more if necessary
	 * 
	 * @author mfrincu
	 */
	public static enum TYPE {
		REQUEST, JOB, DATA
	};

	private TYPE type;

	public TYPE getType() {
		return this.type;
	}

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name of the task
	 * @param computeSize
	 *            the computation requirements of the task in seconds
	 * @param communicationSize
	 *            the network transfer requirements of the task in bytes
	 * @param type
	 *            the type of the task. See SchIaaSTask.TYPE for details
	 */
	public SchIaaSTask(String name, double computeSize,
			double communicationSize, SchIaaSTask.TYPE type) {
		super(name, computeSize, communicationSize);
		this.type = type;
	}

}
