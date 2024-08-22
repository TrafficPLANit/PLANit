package org.goplanit.output.property;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.unit.Unit;
import org.locationtech.jts.geom.Geometry;

/**
 * Template for output property classes which can be included in the output files.
 * <p>
 * All concrete output property classes must be final and extend this class.
 * </p>
 * 
 * @author gman6028, markr
 *
 */
public abstract class OutputProperty implements Comparable<OutputProperty> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OutputProperty.class.getCanonicalName());

  private static final DecimalFormat DEFAULT_FORMAT_NUM_DECIMALS = new DecimalFormat("#.#");

  static {
    DEFAULT_FORMAT_NUM_DECIMALS.setMaximumFractionDigits(7);
    DEFAULT_FORMAT_NUM_DECIMALS.setMinimumFractionDigits(1);
  }

  /** the override units */
  private Unit overrideUnits = null;


  /** Formats an object for legigible printing,
   * if a double or float, outputs value based on {@link #DEFAULT_FORMAT_NUM_DECIMALS}, if null empty string is created,
   * if a geometry then the geometry is converted to a string
   *
   * @param value the value to be output
   * @return the formatted output
   */
  public Object formatValue(Optional<?> value) {
    return formatValue(value.orElse(null));
  }

   /** Formats a value for this property type for legigible printing if so deemed helpful.
    * <p>
    * if a double or float, outputs value based on {@link #DEFAULT_FORMAT_NUM_DECIMALS}, if null empty string is created,
    * if a geometry then the geometry is converted to a string
    </p>
    *
    * @param value the value to be output
    * @return the formatted output
    */
  public Object formatValue(Object value) {
    if (value == null) {
      return "";
    } else if (value instanceof Double) {
      return DEFAULT_FORMAT_NUM_DECIMALS.format((double) value);
    } else if (value instanceof Float) {
      return DEFAULT_FORMAT_NUM_DECIMALS.format((float) value);
    } else if (value instanceof Geometry) {
      /* geometry needs conversion to string for it to be writeable */
      return value.toString();
    }else {
      return value;
    }
  }

  /**
   * Returns the name of the output property
   * 
   * @return name of the output property
   */
  public abstract String getName();

  /**
   * Returns the units of the output property
   * 
   * @return units of the output property
   */
  public abstract Unit getDefaultUnit();

  /**
   * An output property can be allowed to deviate from its default unit. In which case an override unit is to be made available. By default an output property is not allowed to
   * deviate. So derived implementations must override this method to ensure it returns true if it does support this feature.
   * 
   * @return true when allowed, false otherwise
   */
  public boolean supportsUnitOverride() {
    return false;
  }

  /**
   * Indicates if default units are overridden or not.
   * 
   * @return true when overridden, false otherwise
   */
  public boolean isUnitOverride() {
    return overrideUnits != null;
  }

  /**
   * set the units to use for overriding the defaults
   * 
   * @param overrideUnit units to use
   */
  public void setUnitOverride(Unit overrideUnit) {
    if (!supportsUnitOverride()) {
      LOGGER.warning(String.format("IGNORE: overriding default units for output property %s, not allowed", this.getClass().getCanonicalName()));
    }
    if (!getDefaultUnit().canConvertTo(overrideUnit)) {
      LOGGER.warning(String.format("IGNORE: overriding units %s yield unsupported conversion from default units %s for output ptoperty %s", getDefaultUnit().toString(),
          overrideUnit.toString(), this.getClass().getCanonicalName()));
    }
    this.overrideUnits = overrideUnit;
  }

  /**
   * Only when the property allows a unit override and an override is set this method returns the proposed alternative unit to use. These units are required to be convertible from
   * the original default unit to the proposed unit
   * 
   * @return proposed unit, original unit if none is set
   */
  public Unit getOverrideUnit() {
    return isUnitOverride() ? overrideUnits : getDefaultUnit();
  }

  /**
   * Returns the data type of the output property
   * 
   * @return data type of the output property
   */
  public abstract DataType getDataType();

  /**
   * Return the value of the OutputProperty enumeration for this property
   * 
   * @return the value of the OutputProperty enumeration for this property
   */
  public abstract OutputPropertyType getOutputPropertyType();

  /**
   * Gets the column priority of the output property in output files
   * 
   * The lower the column priority value of a property, the further to the left it is placed in the output file
   * 
   * @return the column priority
   */
  public abstract OutputPropertyPriority getColumnPriority();

  /**
   * Overridden equals() method
   * 
   * This method is needed to allow output properties to be removed from the output list if required.
   * 
   * @param otherProperty output property to be compared to this one
   * 
   */
  public boolean equals(Object otherProperty) {
    return this.getClass().getCanonicalName().equals(otherProperty.getClass().getCanonicalName());
  }

  /**
   * Overridden hashCode() method
   * 
   * This method is needed to allow output properties to be removed from the output list if required.
   * 
   */
  public int hashCode() {
    return getDefaultUnit().hashCode() + getDataType().hashCode() + getName().hashCode();
  }

  /**
   * compareTo method used to order the output columns when output is being written
   * 
   * @param otherProperty output property which is being compared to the current one
   */
  public int compareTo(OutputProperty otherProperty) {
    if (getColumnPriority().equals(otherProperty.getColumnPriority())) {
      if (getName().equals(otherProperty.getName())) {
        return getOutputPropertyType().compareTo(otherProperty.getOutputPropertyType());
      } else {
        return getName().compareTo(otherProperty.getName());
      }
    }
    return getColumnPriority().value() - otherProperty.getColumnPriority().value();
  }

  /**
   * Generate the appropriate BaseOutputProperty object from a specified class name
   * 
   * @param propertyClassName the class name of the specified output property
   * @return the BaseOutputProperty object corresponding to the specified enumeration value
   */
  public static OutputProperty of(String propertyClassName){
    try {
      Class<?> entityClass = Class.forName(propertyClassName);
      OutputProperty outputProperty = (OutputProperty) entityClass.getDeclaredConstructor().newInstance();
      return outputProperty;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException(String.format("Error when converting base output property %s", propertyClassName), e);
    }
  }

  /**
   * Generate the appropriate BaseOutputProperty object from a specified enumeration value
   * 
   * @param outputProperty the enumeration value of the specified output property
   * @return the BaseOutputProperty object corresponding to the specified enumeration value
   */
  public static OutputProperty of(OutputPropertyType outputProperty) {
    return of(outputProperty.value());
  }

  /**
   * Utility method to verify if any of the provided output properties are of the provided type
   *
   * @param outputProperties to check
   * @param type(s) to check
   * @return true if at least one property matches with one of the types to check against, false, otherwise
   */
  public static boolean containsPropertyOfType(OutputProperty[] outputProperties, OutputPropertyType... type){
    return Arrays.stream(outputProperties).anyMatch(op ->
        Arrays.stream(type).anyMatch(t -> t.equals(op.getOutputPropertyType())));
  }

  /**
   * Utility method to verify if any of the provided output properties contain any ID type for the time period (ID, XML_ID, EXTERNAL_ID)
   *
   * @param outputProperties to check
   * @return true if at least one property matches with a time period id type
   */
  public static boolean containsAnyTimePeriodIdType(OutputProperty[] outputProperties){
    return containsPropertyOfType(
        outputProperties, OutputPropertyType.TIME_PERIOD_XML_ID, OutputPropertyType.TIME_PERIOD_ID, OutputPropertyType.TIME_PERIOD_EXTERNAL_ID);
  }

  /**
   * Utility method to verify if any of the provided output properties contain any ID type for the mode (ID, XML_ID, EXTERNAL_ID)
   *
   * @param outputProperties to check
   * @return true if at least one property matches with a mode id type
   */
  public static boolean containsAnyModeIdType(OutputProperty[] outputProperties){
    return containsPropertyOfType(
        outputProperties, OutputPropertyType.MODE_XML_ID, OutputPropertyType.MODE_ID, OutputPropertyType.MODE_EXTERNAL_ID);
  }

}
