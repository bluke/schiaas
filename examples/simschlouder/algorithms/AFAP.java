package simschlouder.algorithms;

import simschlouder.SchloudCloud;
import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The As Full As Possible strategy: TODO describe it
 * @author mfrincu
 *
 */
public class AFAP extends AStrategy {

	@Override
	public String getName() {
		return "AFAP";
	}

	@Override
	protected SchloudNode applyStrategy(SchloudTask schloudTask) {
		SchloudNode candidate = null;
		
		double candidatePredictedIdleTime = SchloudController.schloudCloud.getBtuTime() + 1;
		
		for (SchloudNode node : SchloudController.nodes) {

			double currentidleTime = node.getRemainingIdleTime();
			double predictedIdleTime = (SchloudController.schloudCloud.getBtuTime()*SchloudController.time2BTU(schloudTask.getWalltimePrediction()+node.getUpTimeToIdle())) - (schloudTask.getWalltimePrediction()+node.getUpTimeToIdle());
			if ( predictedIdleTime < currentidleTime && predictedIdleTime < candidatePredictedIdleTime  ) {
				candidate = node;
				candidatePredictedIdleTime = predictedIdleTime;
			} 
		}
		if( candidate==null && SchloudController.schloudCloud.describeAvailability(SchloudController.instanceTypeId)<=0){
			// we choose the VM with the closest IdleTime.
			for (SchloudNode node : SchloudController.nodes) {
				if(candidate==null){
					candidate=node;
				}
				else
				{
					if(node.getIdleDate()<candidate.getIdleDate()){
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
