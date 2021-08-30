package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Property to provide the Outflow rate in PCU/hour.
 * 
 * @author markr
 *
 */
public final class OutflowOutputProperty extends BaseOutputProperty {

  /**
   * Name of the property
   */
  public static final String NAME = "Outflow";

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
    return OutputProperty.OUTFLOW;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
