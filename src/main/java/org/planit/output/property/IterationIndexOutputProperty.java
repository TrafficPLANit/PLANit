package org.planit.output.property;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.DataType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.unit.Units;

public final class IterationIndexOutputProperty extends OutputProperty {

  public static final String NAME = "Iteration Index";

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
    return DataType.INTEGER;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.ITERATION_INDEX;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the current iteration index
   * 
   * @param trafficAssignment TrafficAssignment containing data which may be required
   * @return the current iteration index
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Integer> getIterationIndex(TrafficAssignment trafficAssignment) throws PlanItException {
    return Optional.of(trafficAssignment.getIterationIndex());
  }

}
