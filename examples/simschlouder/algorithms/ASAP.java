package simschlouder.algorithms;

import java.util.Collections;

import org.simgrid.msg.Msg;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The As Soon As Possible Strategy: TODO describe it
 * @author julien.gossa@unistra.fr
 *
 */
public class ASAP extends AStrategy {

	@Override
	public String getName() {
		return "ASAP";
	}

	@Override
	protected SchloudNode applyStrategy(SchloudTask task) {
		SchloudNode candidate = null;
		SchloudNode finishSooner = null;
	
		Msg.info("ASAP Strategy for "+task);
		
		if (!SchloudController.nodes.isEmpty())
			finishSooner = SchloudController.nodes.firstElement();
		
		for (SchloudNode node : SchloudController.nodes) {
						
			// Look for the first instance to become available
			if (node.getIdleDate() < finishSooner.getIdleDate())
				finishSooner = node;
			
			// Look for the ASAP candidate 
			if (node.getIdleDate() < Msg.getClock()+SchloudController.schloudCloud.getBootTimePrediction()) 
				if ( candidate == null || node.getRemainingIdleTime(task) < candidate.getRemainingIdleTime(task) )
					candidate = node;

			// Track one given task
			//if (task.getName().equals("F025743DA_1.mgf"))  
				Msg.info("ASAP "+node.instanceId+"("+node.getState()+"): "+node.getIdleDate()+"<"+(Msg.getClock()+SchloudController.schloudCloud.getBootTimePrediction())
					+" / "+node.getRemainingIdleTime(task)
					+" - "+finishSooner.instanceId+"\t"+((candidate!=null)?candidate.instanceId:"null"));
		}
		
		// If no new instance can be started
		if (SchloudController.schloudCloud.describeAvailability(SchloudController.instanceTypeId)==0)
				return finishSooner;
		
		return candidate;
	}

	@Override
	protected ORDERING getOrdering() {
		return ORDERING.NONE;
	}


}
