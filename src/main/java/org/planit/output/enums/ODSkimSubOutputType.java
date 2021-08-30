package org.planit.output.enums;

/**
 * Enumeration of possible values of OD Skim Output Type
 * 
 * @author gman6028
 *
 */
public enum OdSkimSubOutputType implements SubOutputTypeEnum {

  NONE("None"),
  COST("Cost");

  private final String value;

  OdSkimSubOutputType(String v) {
    value = v;
  }

  public String value() {
    return value;
  }
}
