package org.goplanit.output.property;

/**
 * Enumeration giving the priority of output properties.
 * 
 * Output values written to a file or stored in memory are stored in columns for each iteration: each iteration is a
 * row in a data table, and the output properties are its columns. The values of this enumeration define where the
 * output columns are placed in the table. The lower the priority value, the further to the left the column is placed.
 * So ID values go to the left, followed by input values, followed by result values.
 * 
 * @author gman6028
 *
 */

public enum OutputPropertyPriority {

  /** Priority for identifier columns. */
  ID_PRIORITY(0),
  /** Priority for input columns. */
  INPUT_PRIORITY(1),
  /** Priority for result columns. */
  RESULT_PRIORITY(2);

  /** Integer priority value. */
  private final int value;

  /**
   * Constructor.
   *
   * @param v integer priority value
   */
  OutputPropertyPriority(int v) {
    value = v;
  }

  /**
   * Return the String value associated with this enumeration value (the fully qualified class name)
   * 
   * @return the class name associated with this enumeration value
   */
  public int value() {
    return value;
  }

}
