package simschlouder.algorithms;

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
		if (!SchloudController.nodes.isEmpty())
			finishSooner = SchloudController.nodes.firstElement();
		
		for (SchloudNode node : SchloudController.nodes) {
			//Msg.info("ASAP : "+(Msg.getClock()+cloud.getBootTimePrediction())+"("+cloud.getBootTimePrediction()+ " " + cloud.bootCount + ") - "+ node.getIdleDate());
			
			// Look for the first instance to become available
			if (node.getIdleDate() < finishSooner.getIdleDate())
				finishSooner = node;
			
			// Look for the ASAP candidate 
			if (node.getIdleDate() < Msg.getClock()+SchloudController.schloudCloud.getBootTimePrediction()) 
				if ( candidate == null || node.getRemainingIdleTime(task) < candidate.getRemainingIdleTime(task) )
					candidate = node;
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
