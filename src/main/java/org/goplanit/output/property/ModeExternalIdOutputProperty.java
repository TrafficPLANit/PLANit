package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;

public final class ModeExternalIdOutputProperty extends OutputProperty {

  public static final String NAME = "Mode External Id";

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
    return DataType.STRING;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.MODE_EXTERNAL_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the external Id of the current mode
   * 
   * @param mode current mode
   * @return the external Id of the current mode
   */
  public static Optional<String> getModeExternalId(Mode mode) {
    return Optional.of(mode.getExternalId());
  }

}
