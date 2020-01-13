package org.planit.utils;

import java.text.DecimalFormat;

/**
 * Utility methods for formatting doubles
 */
public class FormatUtils {

	/**
	 * Formatter to be used to output double to five decimal places
	 */
	private static DecimalFormat df5 = new DecimalFormat("#.#####");

	/**
	 * Formatter to be used to output double to six decimal places
	 */
	private static DecimalFormat df6 = new DecimalFormat("#.######");
	
	/**
	 * Outputs a double to five decimal places
	 * 
	 * @param value  double value to be output
	 * @return String containing the double output to five decimal places
	 */
	public static String format5(double value) {
		return df5.format(value);
	}

	/**
	 * Outputs a double to six decimal places
	 * 
	 * @param value  double value to be output
	 * @return String containing the double output to six decimal places
	 */
	public static String format6(double value) {
		return df6.format(value);
	}
}
