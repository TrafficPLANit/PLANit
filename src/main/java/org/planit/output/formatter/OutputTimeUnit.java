package org.planit.output.formatter;

public enum OutputTimeUnit {

	 HOURS("hr"),
	 MINUTES("min"),
	 SECONDS("sec");
	 private final String value;

	 OutputTimeUnit(String v) {
	     value = v;
	 }

	 public String value() {
	     return value;
	 }
}
