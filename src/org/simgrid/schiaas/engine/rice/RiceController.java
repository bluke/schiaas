package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Instance;

/**
 * Useless for now.
 * @author julien
 *
 */
public class RiceController extends Process {
	
	protected Rice rice;
	
	public RiceController(Rice rice) {
		super(rice.controller, rice.getCompute().getId()+" Rice Controller");
		this.rice = rice;
	} 
	
	public void main(String[] args) throws MsgException {
		for (Instance instance : this.rice.getCompute().describeInstances()) {
			
		}
	}

}
