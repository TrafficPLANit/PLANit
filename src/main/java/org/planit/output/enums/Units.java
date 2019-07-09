package org.planit.output.enums;

/**
 * Enumeration of possible units 
 * 
 * @author gman6028
 *
 */
public enum Units {

	 VEH_KM("VEH_KM"),
	 NONE("NONE"),
	 VEH_H("VEH_H"),
	 KM_H("KM_H"),
	 H("H"),
	 KM("KM"),
	 SRS("SRS");
	 private final String value;

	 Units(String v) {
	     value = v;
	 }

	 public String value() {
	     return value;
	 }
}
