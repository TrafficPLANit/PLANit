package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Mode Id.
 */
public final class ModeIdOutputProperty extends OutputProperty {


  /** Mode Id. */
  public static final String NAME = "Mode Id";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public ModeIdOutputProperty(){}

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
    return DataType.LONG;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.MODE_ID;
  }

  /**
   * {@inheritDoc}
   */
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
