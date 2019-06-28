package org.planit.output.property;

/**
 * Enumeration of possible data types
 * 
 * @author gman6028
 *
 */
public enum Type {

	 DOUBLE("DOUBLE"),
	 FLOAT("FLOAT"),
	 INTEGER("INTEGER"),
	 BOOLEAN("BOOLEAN");
	 private final String value;

	 Type(String v) {
	     value = v;
	 }

	 public String value() {
	     return value;
	 }
}
