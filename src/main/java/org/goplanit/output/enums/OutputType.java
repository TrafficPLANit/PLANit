package org.goplanit.output.enums;

/**
 * Different configurations exist for different types of output which we
 * identify via this enum
 * GENERAL:
 * LINK: link based output on a network wide level
 * SIMULATION: simulation based output such as profile information, objects created etc.
 * OD: origin-destination based output regarding travel times and other costs on an origin-destination based level
 * PATH: Path based output differentiated on individual path level
 * BUSH: link based output on a bush level (assuming the assignment method used supports this output)
 * 
 * @author markr
 */
public enum OutputType implements OutputTypeEnum {
  GENERAL("General"),
  LINK("Link"),
  SIMULATION("Simulation"),
  OD("Origin-Destination"),
  PATH("Path"),
  BUSH("Bush");

  private final String value;

  OutputType(String v) {
    value = v;
  }

  public String value() {
    return value;
  }
}
