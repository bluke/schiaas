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
		double candidatePredictedIdleTime = SchloudController.schloudCloud.getBtuTime() + 1;
		for (SchloudNode node : SchloudController.nodes) {
			//Msg.info("ASAP : "+(Msg.getClock()+cloud.getBootTimePrediction())+"("+cloud.getBootTimePrediction()+ " " + cloud.bootCount + ") - "+ node.getIdleDate());
			
			if (Msg.getClock()+SchloudController.schloudCloud.getBootTimePrediction()>=node.getIdleDate()) {
				double predictedIdleTime = 
					(SchloudController.schloudCloud.getBtuTime()
						*SchloudController.time2BTU(task.getWalltimePrediction()
						+node.getUpTimeToIdle())) 
					- (task.getWalltimePrediction()+node.getUpTimeToIdle());
				if(candidate==null){
					candidate=node;
					candidatePredictedIdleTime = predictedIdleTime;
				}
				else
				{
					// Sub-optimal: should choose idle node over soon-to-be ones
					if(predictedIdleTime<candidatePredictedIdleTime){
						candidate=node;
						candidatePredictedIdleTime = predictedIdleTime;
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
