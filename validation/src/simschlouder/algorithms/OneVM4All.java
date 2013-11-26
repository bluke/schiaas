package simschlouder.algorithms;

import simschlouder.SchloudController;
import simschlouder.SchloudNode;
import simschlouder.SchloudTask;

/**
 * The OneVM4All strategy: use a single VM for all tasks.
 * @author mfrincu
 *
 */
public class OneVM4All extends AStrategy {

	@Override
	public String getName() {		
		return "OneVM4All";
	}

	@Override
	protected SchloudNode applyStrategy(SchloudTask task) {
		if (SchloudController.nodes.size()==0)
			return null;
		return SchloudController.nodes.get(0);
	}

	@Override
	protected ORDERING getOrdering() {
		return ORDERING.NONE;
	}

}
