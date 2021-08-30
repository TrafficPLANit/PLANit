package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Capacity per lane in pcu/h
 * 
 * @author markr
 *
 */
public final class CapacityPerLaneOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Capacity per Lane";

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
    return OutputProperty.CAPACITY_PER_LANE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
