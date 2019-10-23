package org.planit.output.enums;

public enum OutputTimeUnit {

	 HOURS("hr", 1.0),
	 MINUTES("min", 60.0),
	 SECONDS("sec", 3600.0);
	 private final String value;
	 private double multiplier;

	 OutputTimeUnit(String v, double multiplier) {
	     value = v;
	     this.multiplier = multiplier;
	 }
	 
	 public double getMultiplier() {
		 return multiplier;
	 }

	 public String value() {
	     return value;
	 }
}