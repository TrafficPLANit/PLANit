package org.planit.output.enums;

/**
 * Enumeration of possible output data types
 * 
 * @author gman6028
 *
 */
public enum Type {

	 DOUBLE("DOUBLE"),
	 FLOAT("FLOAT"),
	 INTEGER("INTEGER"),
	 LONG("LONG"),
	 BOOLEAN("BOOLEAN"),
	 STRING("STRING"),
	 SRSNAME("SRSNAME");
	 private final String value;

	 Type(String v) {
	     value = v;
	 }

	 public String value() {
	     return value;
	 }
}
