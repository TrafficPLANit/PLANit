package org.planit.output.property;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.Type;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.unit.Units;

public final class IterationIndexOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Iteration Index";

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
    return Type.INTEGER;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.ITERATION_INDEX;
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
