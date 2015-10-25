package simschlouder.algorithms;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The First Fit strategy.
 * Simply schedule the tasks to the first instance that can handle it at constant cost.
 * @author mfrincu 
 */
public class FirstFit extends AStrategy {

	@Override
	public String getName() {
		return "FirstFit";
	}

	@Override
	protected SchloudNode applyStrategy(SchloudTask schloudTask) {
		SchloudNode candidate = null;
		
		
		for (SchloudNode node : SchloudController.nodes) {
			//Msg.info("FirstFit : "+(Msg.getClock()+cloud.getBootTimePrediction())+"("+cloud.getBootTimePrediction()+") - "+ node.getIdleDate());
			if (SchloudController.time2BTU(node.getUpTimeToIdle()) == SchloudController.time2BTU(node.getUpTimeToIdle()+schloudTask.getWalltimePrediction()+SchloudController.schloudCloud.getShutdownMargin())) {
				candidate = node;
				break;
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