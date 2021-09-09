package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

/**
 * Property to provide the Inflow rate in PCU/hour.
 * 
 * @author markr
 *
 */
public final class InflowOutputProperty extends OutputProperty {

  /**
   * Name of the property
   */
  public static final String NAME = "Inflow";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Units getDefaultUnits() {
    return Units.PCU_HOUR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataType getDataType() {
    return DataType.DOUBLE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.INFLOW;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
