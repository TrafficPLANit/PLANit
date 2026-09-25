package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Mode External Id.
 */
public final class ModeExternalIdOutputProperty extends OutputProperty {


  /** Mode External Id. */
  public static final String NAME = "Mode External Id";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public ModeExternalIdOutputProperty(){}

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
  public Unit getDefaultUnit() {
    return Unit.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataType getDataType() {
    return DataType.STRING;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.MODE_EXTERNAL_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the external Id of the current mode
   * 
   * @param mode current mode
   * @return the XML Id of the current mode
   */
  public static Optional<String> getModeExternalId(Mode mode) {
    return Optional.of(mode.getExternalId());
  }

}
