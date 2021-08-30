package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Maximum density in pcu/km
 * 
 * @author markr
 *
 */
public final class MaximumDensityOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Maximum Density";

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
    return Units.PCU_KM;
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
    return OutputProperty.DENSITY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
