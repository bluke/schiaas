package org.simgrid.schiaas.billing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.simgrid.msg.Msg;

/**
 * Some utilities for handling billing
 * @author mfrincu
 *
 */
public class BillingUtil {
	
	public static double getPrice(Map<String, Double> list, double key) {
		Set<String> keys = list.keySet();
		String[] vals;
		for (String k : keys) {
			vals = k.split("#");
			// both limits are -1 and a single element is in the list return
			// this price
			if (Double.parseDouble(vals[0]) == -1
					&& Double.parseDouble(vals[1]) == -1
					&& keys.size() == 1) {
				System.out.println(key);
				return list.get(k);
			}
			// key is between the limits => return this price
			if (key >= Double.parseDouble(vals[0])
					&& key <= Double.parseDouble(vals[1])) {
				return list.get(k);
			}
			// key is between a value and -1 (infinity) => return this price
			if (key >= Double.parseDouble(vals[0])
					&& Double.parseDouble(vals[1]) == -1) {
				return list.get(k);
			}
		}
		return 0;
	}
	
	public static Map<String, Double> readFile(String filename) {
		Map<String, Double> vals = new HashMap<String, Double>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			Msg.critical("Error trying to read: " + filename + ". "
					+ e.getMessage());
			return null;
		}
		String line = null;
		String[] elems = null;
		String key = null;
		try {
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				elems = line.split(",");
				if (elems.length != 3) {
					Msg.error("Looking for 3 items, found: " + elems.length
							+ " on line: " + line + " file: " + filename);
				}
				key = elems[0] + "#" + elems[1];

				if (vals.containsKey(key)) {
					Msg.error("Storage price already exists. Replacing existing one. File: "
							+ filename);
				}
				vals.put(key, Double.parseDouble(elems[2]));
			}
		} catch (IOException e) {
			Msg.critical("Error while reading file: " + filename + ". "
					+ e.getMessage());
			return null;
		}

		try {
			reader.close();
		} catch (IOException e) {
			Msg.critical("Error closing file: " + filename + ". "
					+ e.getMessage());
		}
		return vals;
	}
}
