package org.goplanit.output.enums;

/**
 * Enumeration of possible values of OD Skim Output Type
 * 
 * @author gman6028
 *
 */
public enum SkimSubOutputType implements SubOutputTypeEnum {

  /** No skim sub-output type selected. */
  NONE("None"),
  /** Travel time skim output. */
  TRAVEL_TIME("TravelTime"),
  /** Distance skim output. */
  DISTANCE("Distance"),
  /** Generalized or monetary cost skim output. */
  COST("Cost");

  /** String label for this skim sub-output type. */
  private final String value;

  /**
   * Constructor.
   *
   * @param v string label for the skim sub-output type
   */
  SkimSubOutputType(String v) {
    value = v;
  }

  /**
   * Access the string label for this skim sub-output type.
   *
   * @return skim sub-output type label
   */
  public String value() {
    return value;
  }
}
