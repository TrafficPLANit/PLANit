package org.planit.output.property;

import java.util.Optional;

import org.planit.output.enums.DataType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.unit.Unit;

public final class TimePeriodExternalIdOutputProperty extends OutputProperty {

  public static final String NAME = "Time Period External Id";

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
