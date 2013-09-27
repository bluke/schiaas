package org.simgrid.schiaas;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Task;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.billing.NetworkBilling;
import org.w3c.dom.Node;
import org.simgrid.schiaas.process.SchIaaSTask;

//TODO implement functionality and add XML structure in cloud file

public class Network {

	protected String id;
	protected Cloud cloud;

	protected double billCost;

	/**
	 * @return the total transfer cost induced by communication originating in
	 *         the cloud this network belongs to.
	 */
	public double getTransferCost() {
		return this.billCost;
	}

	/**
	 * 
	 * @param cloud
	 * @param networkXMLNode
	 * @TODO got to do it, and no idea how.
	 */
	public Network(Cloud cloud, Node networkXMLNode) {
		this.cloud = cloud;
	}

	public String getId() {
		return this.id;
	}

	public void terminate() {
		// TODO Auto-generated method stub

	}

	// TODO the following is experimental and could be moved somewhere else
	// TODO for now we only use the GB as billing unit. Kind of natural but who knows? 
	/**
	 * Sends a task to a given mailbox.
	 * 
	 * @param t
	 *            the task
	 * @param mailbox
	 *            the mailbox destination (e.g., the VM instance ID of the destination)
	 * @throws TransferFailureException
	 * @throws HostFailureException
	 * @throws TimeoutException
	 */
	public void sendTask(Task t, String mailbox)
			throws TransferFailureException, HostFailureException,
			TimeoutException {
		for (String key : SchIaaS.clouds.keySet()) {
			Instance dstInstance = SchIaaS.getCloud(key).getCompute()
					.describeInstance(mailbox);
			if (dstInstance == null) {
				continue;
			}
			//Networks only use GIGABYTE for now
			//Storage uses GYGABYTE and REQUEST
			NetworkBilling.NETWORK_UNIT unit = NetworkBilling.NETWORK_UNIT.GIGABYTE;
			double size = t.getMessageSize() / 1073741824; // convert bytes to GB
			if (t instanceof SchIaaSTask) {
				//TODO change unit & size based on SchIaaSTask.TYPE here (see Storage.java for an example)
				// unit = ...
				// size = ...
			}		
			// All in one: add cost for outgoing and incoming, if applicable
			if (this.cloud.getId().compareToIgnoreCase(key) == 0) {
				this.billCost += this.cloud.getNetworkBillingPolicies()
						.getPolicy(unit)
						.getOutgoingPrice(size);
				SchIaaS.getCloud(key).getNetwork().billCost += SchIaaS
						.getCloud(key).getNetworkBillingPolicies()
						.getPolicy(unit)
						.getIncomingPrice(size);
			}
		}

		t.send(mailbox);
	}

}
