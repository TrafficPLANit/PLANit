package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.utils.time.TimePeriod;
import org.goplanit.output.enums.DataType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.unit.Unit;

public final class TimePeriodIdOutputProperty extends OutputProperty {

  public static final String NAME = "Time Period Id";

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
    return OutputPropertyType.TIME_PERIOD_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the current time period Id
   * 
   * @param timePeriod current time period
   * @return the current time period Id
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getTimePeriodId(TimePeriod timePeriod) throws PlanItException {
    return Optional.of(timePeriod.getId());
  }

}
