package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.unit.Units;

public final class ModeIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Mode Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getUnits() {
    return Units.NONE;
  }

  @Override
  public Type getType() {
    return Type.LONG;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.MODE_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the Id of the current mode
   * 
   * @param mode current mode
   * @return the Id of the current mode
   * @throws PlanItException thrown if there is an error
   */
  public static long getModeId(Mode mode) throws PlanItException {
    return mode.getId();
  }

}
