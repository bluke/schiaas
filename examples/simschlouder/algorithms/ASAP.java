package simschlouder.algorithms;

import org.simgrid.msg.Msg;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The As Soon As Possible Strategy: TODO describe it
 * @author mfrincu
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
		for (SchloudNode node : SchloudController.nodes) {
			//Msg.info("ASAP : "+(Msg.getClock()+cloud.getBootTimePrediction())+"("+cloud.getBootTimePrediction()+ " " + cloud.bootCount + ") - "+ node.getIdleDate());
			
			if (Msg.getClock()+SchloudController.schloudCloud.getBootTimePrediction()>=node.getIdleDate()) {
				if(candidate==null){
					candidate=node;
				}
				else
				{
					// Sub-optimal: should choose idle node over soon-to-be ones
					//if(predictedIdleTime<candidatePredictedIdleTime){
					if ( (candidate.getIdleDate() > node.getIdleDate()) ) { //|| (!candidate.isIdle() && node.isIdle()) )  {
						candidate=node;
					}
				}
			}
		}
		
		return candidate;
	}

	@Override
	protected ORDERING getOrdering() {
		return ORDERING.NONE;
	}


}
