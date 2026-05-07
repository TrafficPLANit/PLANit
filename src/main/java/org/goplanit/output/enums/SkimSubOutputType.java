package org.goplanit.output.enums;

/**
 * Enumeration of possible values of OD Skim Output Type
 * 
 * @author gman6028
 *
 */
public enum SkimSubOutputType implements SubOutputTypeEnum {

  NONE("None"),
  TRAVEL_TIME("TravelTime"),
  DISTANCE("Distance"),
  COST("Cost");

  private final String value;

  SkimSubOutputType(String v) {
    value = v;
  }

  public String value() {
    return value;
  }
}
