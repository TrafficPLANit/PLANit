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
  /** General output not tied to a specific network or demand entity. */
  GENERAL("General"),
  /** Link-based output on the network level. */
  LINK("Link"),
  /** Simulation-level output such as run summaries and profiling. */
  SIMULATION("Simulation"),
  /** Origin-destination based output. */
  OD("Origin-Destination"),
  /** Path-based output for individual paths. */
  PATH("Path"),
  /** Bush-based output for assignment methods that support bushes. */
  BUSH("Bush");

  /** String label for this output type. */
  private final String value;

  /**
   * Constructor.
   *
   * @param v string label for the output type
   */
  OutputType(String v) {
    value = v;
  }

  /**
   * Access the string label for this output type.
   *
   * @return output type label
   */
  public String value() {
    return value;
  }
}
