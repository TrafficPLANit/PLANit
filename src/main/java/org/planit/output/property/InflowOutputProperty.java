package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Property to provide the Inflow rate in PCU/hour.
 * 
 * @author markr
 *
 */
public final class InflowOutputProperty extends BaseOutputProperty {

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
  public Units getUnits() {
    return Units.PCU_HOUR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return Type.DOUBLE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.INFLOW;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
