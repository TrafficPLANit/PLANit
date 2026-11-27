package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Time Period External Id.
 */
public final class TimePeriodExternalIdOutputProperty extends OutputProperty {


  /** Time Period External Id. */
  public static final String NAME = "Time Period External Id";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public TimePeriodExternalIdOutputProperty(){}

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
    return OutputPropertyType.TIME_PERIOD_EXTERNAL_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the current time period external Id
   * 
   * @param timePeriod current time period
   * @return the current time period external Id
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getTimePeriodExternalId(TimePeriod timePeriod) throws PlanItException {
    return Optional.of(timePeriod.getExternalId());
  }

}
