package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.unit.Units;

public final class TimePeriodXmlIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Time Period Xml Id";

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
    return Type.STRING;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.TIME_PERIOD_XML_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the current time period xml Id
   * 
   * @param timePeriod current time period
   * @return the current time period external Id
   * @throws PlanItException thrown if there is an error
   */
  public static Object getTimePeriodXmlId(TimePeriod timePeriod) throws PlanItException {
    return timePeriod.getXmlId();
  }

}
