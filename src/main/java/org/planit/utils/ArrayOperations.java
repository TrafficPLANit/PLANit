package org.planit.utils;

public class ArrayOperations {

	/** add second array its entries element-wise to the first array
	 * @param destination
	 * @param addToDestination
	 * @param numberOfElements
	 */
	public static void addTo(double[] destination, double[] addToDestination, int numberOfElements) {
		for(int index=0;index<numberOfElements;++index) {
			destination[index] += addToDestination[index];
		}
	}
	
	/** add second array its entries element-wise to the first array
	 * @param destination
	 * @param addToDestination
	 * @param numberOfElements
	 */
	public static double sumProduct(double[] d1, double[] d2, int numberOfElements) {
		double sum = 0.0;
		for (int index=0; index<numberOfElements; ++index) {
			sum += d1[index] * d2[index];
		}
		return sum;
	}	
}
