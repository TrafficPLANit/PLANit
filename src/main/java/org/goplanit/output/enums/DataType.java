package org.goplanit.output.enums;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.math.Precision;

import java.util.Comparator;

/**
 * Enumeration of possible output data types
 * 
 * @author gman6028
 *
 */
public enum DataType {

  DOUBLE("DOUBLE"),
  FLOAT("FLOAT"),
  INTEGER("INTEGER"),
  LONG("LONG"),
  BOOLEAN("BOOLEAN"),
  STRING("STRING"),
  SRSNAME("SRSNAME");
  
  private final String value;

  DataType(String v) {
    value = v;
  }

  public String value() {
    return value;
  }


  /**
   * Create default comparator for the data type (based on natural ordering where possible and no
   * precision for precision based data types
   *
   * @return created comparator
   */
  public Comparator<?> getDefaultComparator(){
    return getDefaultComparator(this);
  }

  /**
   * Check if data type is precision based, i.e., float or double
   *
   * @return true when has precision, false otherwise
   */
  public boolean isPrecisionBased(){
    switch (this) {
      case FLOAT:
      case DOUBLE:
        return true;
      default:
        return false;
    }
  }

  /**
   * Create comparator for given data type using natural order
   *
   * @param dataType to use
   * @return comparator created
   */
  public static Comparator<?> getDefaultComparator(DataType dataType){
    // for now always natural order
    switch (dataType) {
      default:
        return Comparator.naturalOrder();
    }
  }

  /**
   * Create comparator for precision based data type using a double based precision approach
   *
   * @param dataType to use
   * @return comparator created
   */
  public static Comparator<Object> getPrecisionComparator(DataType dataType, double epsilon) {
    switch (dataType) {
      case FLOAT:
      case DOUBLE:
        if (!dataType.isPrecisionBased()) {
          throw new PlanItRunTimeException("Unable to create precision based comaprator for data type %s which is not precision based",
              dataType);
        }
        // ensure that we do not need to know the type, we allow for hard cast upon comaprison being conducted
        return Precision.createComparatorWithCast(epsilon);
      default:
        throw new PlanItRunTimeException("Unsupported data type %s, for creating precision comparator for", dataType);
    }
  }
}
