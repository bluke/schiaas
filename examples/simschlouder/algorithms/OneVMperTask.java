package simschlouder.algorithms;

import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The OneVMperTask strategy: schedules each task on a new VM
 * @author mfrincu
 *
 */
public class OneVMperTask extends AStrategy {

	@Override
	public String getName() {
		return "OneVMperTask";
	}

	@Override
	protected SchloudNode applyStrategy(SchloudTask task) {
		return null;
	}

	@Override
	protected ORDERING getOrdering() {
		return ORDERING.NONE;
	}

}
