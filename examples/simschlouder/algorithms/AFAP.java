package simschlouder.algorithms;

import org.simgrid.msg.Msg;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The As Full As Possible strategy.
 * This strategy aims at filling the provisioned instances as most as possible.
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
		
		Msg.info("AFAP Strategy for "+task);
	
		for (SchloudNode node : SchloudController.nodes) {

			// Look for the first instance to become available
			if (task.getName().equals("2mass_pleiades_j_2x2_diff_0")) {  
				Msg.verb("sooner "+node.instance.getId()+":"+node.getIdleDate()+" - "+finishSooner.instance.getId()+":"+finishSooner.getIdleDate());
				if (candidate != null)
					Msg.verb("afap "+node.instance.getId()+":"+node.getRemainingIdleTime()+" "+node.getRemainingIdleTime(task)+" - "+candidate.instance.getId()+":"+candidatePredictedIdleTime);
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
			if ( predictedIdleTime <= currentIdleTime )
				if ( candidate == null ||  predictedIdleTime < candidatePredictedIdleTime ) {
					candidate = node;
					candidatePredictedIdleTime = predictedIdleTime;
				} 
			
			Msg.verb("AFAP "+node.instance.getId()+"("+node.getState()+"): "+currentIdleTime+" >= "+predictedIdleTime
					+" < "+candidatePredictedIdleTime
					+" - "+finishSooner.instance.getId()+"\t"+((candidate!=null)?candidate.instance.getId():"null"));

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
