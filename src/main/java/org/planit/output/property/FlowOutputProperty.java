package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Property to provide the Flow rate in PCU/hour. This property is only to be used when both the inflow rate and outflow rate are guaranteed to be the same. If not, then it depends
 * on the assignment method what this property represents (inflow rate or outflow rate)
 * 
 * @author markr
 *
 */
public final class FlowOutputProperty extends BaseOutputProperty {

  /**
   * Name of the property
   */
  public static final String NAME = "Flow";

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
    return OutputProperty.FLOW;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
