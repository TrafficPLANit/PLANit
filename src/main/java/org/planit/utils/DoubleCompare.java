package org.planit.utils;

/**
 * compare doubles based on what type of value they hold and according epsilons provided
 * @author markr
 *
 */
public class DoubleCompare {
	
	public static final double FLOW_EPSILON = 0.000001;
	
	public static final double DENSITY_EPSILON = 0.000001;
	
	public static boolean flowEquals(double d1, double d2) {
		return Math.abs(d1-d2)<=FLOW_EPSILON;
	}
	
	public static boolean flowGreaterThan(double d1, double d2) {
		return (d1-FLOW_EPSILON)>d2;
	}	
	
	public static boolean densityEquals(double d1, double d2) {
		return Math.abs(d1-d2)<=DENSITY_EPSILON;
	}
	
	public static boolean densityGreaterThan(double d1, double d2) {
		return (d1-DENSITY_EPSILON)>d2;
	}		

}
