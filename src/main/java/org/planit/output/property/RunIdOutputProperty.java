package org.planit.output.property;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.DataType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.unit.Units;

public final class RunIdOutputProperty extends OutputProperty {

  public static final String NAME = "Run Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getDefaultUnits() {
    return Units.NONE;
  }

  @Override
  public DataType getDataType() {
    return DataType.LONG;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.RUN_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the current run id
   * 
   * @param trafficAssignment TrafficAssignment containing data which may be required
   * @return the current run id
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getRunId(TrafficAssignment trafficAssignment) throws PlanItException {
    return Optional.of(trafficAssignment.getId());
  }

}
