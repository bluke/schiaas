package simschlouder.algorithms;

import org.simgrid.msg.Msg;

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
	protected SchloudNode applyStrategy(SchloudTask task) {
		SchloudNode candidate = null;
		SchloudNode finishSooner = null;
		if (!SchloudController.nodes.isEmpty())
			finishSooner = SchloudController.nodes.firstElement();
		
		double candidatePredictedIdleTime = SchloudController.schloudCloud.getBtuTime();
		
		for (SchloudNode node : SchloudController.nodes) {

			// Look for the first instance to become available
			if (task.getName().equals("2mass_pleiades_j_2x2_diff_0")) {  
				Msg.info("sooner "+node.instanceId+":"+node.getIdleDate()+" - "+finishSooner.instanceId+":"+finishSooner.getIdleDate());
				if (candidate != null)
					Msg.info("afap "+node.instanceId+":"+node.getRemainingIdleTime()+" "+node.getRemainingIdleTime(task)+" - "+candidate.instanceId+":"+candidatePredictedIdleTime);
			}
			
				if (node.getIdleDate() <= finishSooner.getIdleDate()) {
			//if (node.getUpTimeToIdle() <= finishSooner.getUpTimeToIdle()) {
				//Msg.info("sooner");
				finishSooner = node;
			}

			double currentIdleTime = node.getRemainingIdleTime();
			double predictedIdleTime = node.getRemainingIdleTime(task);
			
			//Msg.info(predictedIdleTime+"<"+currentidleTime+" && "+predictedIdleTime+"<"+candidatePredictedIdleTime);
			//  <= After bugfix
			if ( predictedIdleTime < currentIdleTime )
				if ( candidate == null ||  predictedIdleTime <= candidatePredictedIdleTime ) {
					candidate = node;
					candidatePredictedIdleTime = predictedIdleTime;
				} 
		}
		
		//Msg.info("Candidate for "+task.getName()+" is "+candidate+" ("+finishSooner+")");
		
		if( candidate==null && SchloudController.schloudCloud.describeAvailability(SchloudController.instanceTypeId)<=0){
			// we choose the VM with the closest IdleTime.
			return finishSooner;
		}
		return candidate;
	}

	@Override
	protected ORDERING getOrdering() {
		return ORDERING.NONE;
	}

}
