package org.simgrid.schiaas.billing;

import java.util.HashMap;
import java.util.Map;

import org.simgrid.msg.Msg;

/**
 * Holds information regarding the billing for the network transfer
 * 
 * @author mfrincu
 * 
 */
public class NetworkBilling {

	public enum NETWORK_UNIT {
		GIGABYTE
	}

	private Map<String, Policy> policies = null;

	public Map<String, Policy> getPolicies() {
		return this.policies;
	}

	public Policy getPolicy(NETWORK_UNIT nu) {
		return this.policies.get(nu.toString());
	}

	public NetworkBilling() {
		this.policies = new HashMap<String, Policy>();
	}

	/**
	 * Each cloud can have various policies for network traffic. For instance
	 * they can have a policy for billing each GB
	 * 
	 * @param unit
	 *            the measurement unit used for the policy. See
	 *            NetworkBilling.NETWORK_UNIT
	 * @param outgoingPriceFile
	 *            the file containing the prices for outgoing data. The format
	 *            is
	 * @param incomingPriceFile
	 *            the file containing the prices for incoming data
	 */
	public void addNetworkBillingPolicy(String unit, String outgoingPriceFile,
			String incomingPriceFile) {
		if (this.policies.containsKey(unit)) {
			Msg.error("Duplicate billing policy for unit: " + unit
					+ ". Replacing.");
		}
		try {
			this.policies.put(unit, new Policy(outgoingPriceFile,
					incomingPriceFile));
		} catch (IllegalArgumentException e) {
			Msg.error("Could not find unit " + unit
					+ " for network billing. Skipping: " + e.getMessage());
		}
	}

	/**
	 * Holds a policy regarding incoming and outgoing network traffic for a
	 * cloud
	 * 
	 * @author mfrincu
	 * 
	 */
	public class Policy {

		private Map<String, Double> outgoingPrices = null,
				incomingPrices = null;

		/**
		 * This method receives a numerical value and fits it inside the keys
		 * for outgoing prices
		 * 
		 * @param key
		 *            a double value representing the size of the transfer
		 * @return the price for the given size or 0 in case there is no policy
		 *         specified
		 */
		public Double getOutgoingPrice(double key) {
			if (this.outgoingPrices == null) {
				return 0.;
			}
			return BillingUtil.getPrice(this.outgoingPrices, key);
		}

		/**
		 * This method receives a numerical value and fits it inside the keys
		 * for incoming prices
		 * 
		 * @param key
		 *            a double value representing the size of the transfer
		 * @return the price for the given size or 0 in case there is no policy
		 *         specified
		 */
		public Double getIncomingPrice(double key) {
			if (this.incomingPrices == null) {
				return 0.;
			}
			return BillingUtil.getPrice(this.incomingPrices, key);

		}

		public Policy(String outgoingPriceFile, String incomingPriceFile) {
			this.outgoingPrices = this.getOutgoingPrices(outgoingPriceFile);
			this.incomingPrices = this.getIncomingPrices(incomingPriceFile);
		}

		private Map<String, Double> getOutgoingPrices(String outgoingPriceFile) {
			if (outgoingPriceFile != null && outgoingPriceFile.length() > 0) {
				return BillingUtil.readFile(outgoingPriceFile);
			}
			return null;
		}

		private Map<String, Double> getIncomingPrices(String incomingPriceFile) {
			if (incomingPriceFile != null && incomingPriceFile.length() > 0) {
				return BillingUtil.readFile(incomingPriceFile);
			}
			return null;
		}
	}
}
