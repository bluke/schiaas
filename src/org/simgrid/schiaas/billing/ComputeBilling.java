package org.simgrid.schiaas.billing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.simgrid.msg.Msg;

/**
 * Holds information regarding the billing for the compute element
 * 
 * @author mfrincu
 * 
 */
final public class ComputeBilling {

	private double fixedPrice;
	private int btu;

	List<Double> dynamicPrice = null;

	/**
	 * Default constructor
	 * 
	 * @param fixedPrice
	 *            the fixed price for an instance type. Paid per BTU
	 * @param btu
	 *            the Bid Time Unit (BTU) used as unit for the fixed price
	 * @param dynamicFilePrice
	 *            the dynamic price for an instance type. Paid per BTU
	 * @param outgoingPriceFile
	 *            the price paid for moving data out of
	 * @param incomingPriceFile
	 */
	public ComputeBilling(Double fixedPrice, int btu, String dynamicFilePrice) {

		this.fixedPrice = fixedPrice;
		this.btu = btu;

		if (dynamicFilePrice != null && dynamicFilePrice.length() > 0) {
			this.dynamicPrice = this.getDynamicPrice(dynamicFilePrice);
		}
	}

	/**
	 * Reads the file containing the dynamic pricing. This is similar to
	 * Amazon's spot instances. The format of the file is number1,number2,...
	 * where numberi represents a price value. The list can be used for
	 * selecting a random price value at runtime
	 * 
	 * @param dynamicFilePrice
	 * @return a list of prices for an instance type
	 */
	private List<Double> getDynamicPrice(String dynamicFilePrice) {
		List<Double> vals = new Vector<Double>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(dynamicFilePrice));
		} catch (FileNotFoundException e) {
			Msg.critical("Error trying to read: " + dynamicFilePrice + ". "
					+ e.getMessage());
			return null;
		}
		String line = null;
		String[] elems = null;
		try {
			while ((line = reader.readLine()) != null) {
				elems = line.split(",");
				for (String e : elems) {
					vals.add(new Double(e));
				}
			}
		} catch (IOException e) {
			Msg.critical("Error while reading file: " + dynamicFilePrice + ". "
					+ e.getMessage());
			return null;
		}

		try {
			reader.close();
		} catch (IOException e) {
			Msg.critical("Error closing file: " + dynamicFilePrice + ". "
					+ e.getMessage());
		}

		return vals;
	}

	public double getFixedPrice() {
		return this.fixedPrice;
	}

	public int getBtu() {
		return this.btu;
	}

	public List<Double> getDynamicPrice() {
		return this.dynamicPrice;
	}

}
