package simschlouder.algorithms;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;


/**
 * The AllParExceed strategy: each task on the same level of parallelism will be executed
 * at the same time on different VMs
 * @author mfrincu
 *
 */
public class AllParExceed extends AStrategy {

	@Override
	public String getName() {		
		return "AllParExceed";
	}

	@Override
	protected SchloudNode applyStrategy(SchloudTask task) {		
		//get the next VM that is ready
		for (SchloudNode node : SchloudController.nodes) {
			if (node.isIdle()) {
				return node;
			}
		}
		
		return null;
	}

	@Override
	protected ORDERING getOrdering() {
		return ORDERING.LEVEL_RANKING;
	}

}
