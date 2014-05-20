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
			if (node.isIdle() || (Msg.getClock()+SchloudController.schloudCloud.getBootTimePrediction()>=node.getIdleDate())) {
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
