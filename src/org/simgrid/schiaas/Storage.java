package org.simgrid.schiaas;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Task;
import org.simgrid.msg.TimeoutException;
import org.simgrid.msg.TransferFailureException;
import org.simgrid.schiaas.billing.StorageBilling;
import org.simgrid.schiaas.process.SchIaaSTask;
import org.w3c.dom.Node;

//TODO implement functionality and add XML structure in cloud file

public class Storage {

	protected String id;
	protected Cloud cloud;

	/** stores the policies for the billing of storage operations on this cloud */
	protected StorageBilling storageBilling = new StorageBilling();

	protected double networkBillCost;

	/**
	 * @return the total cost for operations made on this storage
	 */
	public double getStorageTransferCost() {
		return this.networkBillCost + this.crtRequestCost;
	}

	/**
	 * @return a list of policies for the storage billing
	 */
	public StorageBilling getStorageBillingPolicies() {
		return this.storageBilling;
	}

	/**
	 * 
	 * @param cloud
	 * @param storageXMLNode
	 *            TODO got to do it,, need time to develop the STOMAC - STOrage
	 *            MinimAl Component.
	 */
	public Storage(Cloud cloud, Node storageXMLNode) {
		this.cloud = cloud;
		this.id = storageXMLNode.getAttributes().getNamedItem("id")
				.getNodeValue();
	}

	public String getId() {
		return this.id;
	}

	public void terminate() {
		// TODO Auto-generated method stub

	}

	private int noTotalRequests = 0;
	private double crtRequestCost = 0;
	private double crtStoredData = 0;
	private static double BYTES_TO_GB = 1073741824;

	/**
	 * Send a request to this storage. Similar to Amazon's S3 PUT, DELETE, etc.
	 * TODO differentiate between various request types
	 * 
	 * @param r
	 *            the request
	 */
	public void makeRequest(Task r) {
		this.noTotalRequests++;
		if (r instanceof SchIaaSTask) {
			if (((SchIaaSTask) r).getType() == SchIaaSTask.TYPE.REQUEST) {
				this.crtRequestCost = this.getStorageBillingPolicies()
						.getPolicy(StorageBilling.STORAGE_UNIT.REQUEST)
						.getIncomingPrice(this.noTotalRequests);
			}
		}
	}

	/**
	 * Moves data from this storage. It is made of two parts. getting data out
	 * of storage and moving it to the destination. Moving to the destination
	 * does not mean putting it inside another storage (for now). To do this use
	 * the moveDataIn of the corresponding storage element
	 * 
	 * @param t
	 *            the data to be moved out
	 * @param mailbox
	 *            the destination of the data
	 * @throws TimeoutException
	 * @throws HostFailureException
	 * @throws TransferFailureException
	 */
	public void moveDataOut(Task t, String mailbox)
			throws TransferFailureException, HostFailureException,
			TimeoutException {
		if (t instanceof SchIaaSTask) {
			if (((SchIaaSTask) t).getType() == SchIaaSTask.TYPE.DATA
					|| ((SchIaaSTask) t).getType() == SchIaaSTask.TYPE.JOB) {
				// only send if mailbox is valid
				this.networkBillCost += this
						.getStorageBillingPolicies()
						.getPolicy(StorageBilling.STORAGE_UNIT.GIGABYTE)
						.getOutgoingPrice(
								t.getMessageSize() / Storage.BYTES_TO_GB); // convert
				// bytes
				// to
				// GB);
				if (mailbox != null && mailbox.trim().length() > 0) {
					this.cloud.network.sendTask(t, mailbox);
				}
				this.crtStoredData -= t.getMessageSize();
				if (this.crtStoredData < 0) {
					this.crtStoredData = 0;
					Msg.error("Oops. We have moved more data than available. Please check the size of data transfers.");
				}
			}
		}
	}

	/**
	 * Takes some data out of the storage device. It lets the user handle where
	 * the data will go
	 * 
	 * @param t
	 *            the data to be moved out
	 * @throws TimeoutException
	 * @throws HostFailureException
	 * @throws TransferFailureException
	 */
	public void moveDataOut(Task t) throws TransferFailureException,
			HostFailureException, TimeoutException {
		this.moveDataOut(t, null);
	}

	/**
	 * Moves data in this storage. TODO check if this can be removed. Make the
	 * user's life easier
	 * 
	 * @param t
	 *            the data to be moved in
	 */
	public void moveDataIn(Task t) {
		if (t instanceof SchIaaSTask) {
			if (((SchIaaSTask) t).getType() == SchIaaSTask.TYPE.DATA
					|| ((SchIaaSTask) t).getType() == SchIaaSTask.TYPE.JOB) {
				this.networkBillCost += this
						.getStorageBillingPolicies()
						.getPolicy(StorageBilling.STORAGE_UNIT.GIGABYTE)
						.getIncomingPrice(
								t.getMessageSize() / Storage.BYTES_TO_GB); // convert
				// bytes
				// to
				// GB);
				this.crtStoredData += t.getMessageSize();
			}
		}
	}

	/**
	 * 
	 * @return the current cost for stored data
	 */
	public double getCurrentStoredDataCost() {
		return this.getStorageBillingPolicies()
				.getPolicy(StorageBilling.STORAGE_UNIT.GIGABYTE)
				.getStoragePrice(this.crtStoredData / Storage.BYTES_TO_GB);
	}
}
