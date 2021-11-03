package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;

public final class ModeIdOutputProperty extends OutputProperty {

  public static final String NAME = "Mode Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Unit getDefaultUnit() {
    return Unit.NONE;
  }

  @Override
  public DataType getDataType() {
    return DataType.LONG;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.MODE_ID;
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
  public static Optional<Long> getModeId(Mode mode) throws PlanItException {
    return Optional.of(mode.getId());
  }

}
