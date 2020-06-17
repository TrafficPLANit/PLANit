package org.planit.output.property;

/**
 * Enumeration giving the priority of output properties.
 * 
 * If output values are to be written to a file or stored in memory, they are stored in columns for
 * each iteration.
 * Each iteration represents a row in a data table, with the output properties representing the
 * columns. The values of this
 * enumeration define where the output columns are placed in the table. The lower the priority
 * value, the further to
 * the left the column is placed. So values which represent ID values go to the left, followed by
 * values which
 * represent input values, followed by columns which represent result values.
 * 
 * @author gman6028
 *
 */

public enum OutputPropertyPriority {

  ID_PRIORITY(0),
  INPUT_PRIORITY(1),
  RESULT_PRIORITY(2);

  private final int value;

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
