package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;

public final class TimePeriodIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Time Period Id";

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
    return OutputProperty.TIME_PERIOD_ID;
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
  public static long getTimePeriodId(TimePeriod timePeriod) throws PlanItException {
    return timePeriod.getId();
  }

}
