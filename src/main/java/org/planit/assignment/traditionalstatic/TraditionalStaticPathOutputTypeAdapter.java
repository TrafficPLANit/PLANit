package org.planit.assignment.traditionalstatic;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.adapter.PathOutputTypeAdapterImpl;
import org.planit.output.enums.OutputType;
import org.planit.utils.mode.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for OD path outputs without exposing the internals of the traffic assignment class itself
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticPathOutputTypeAdapter extends PathOutputTypeAdapterImpl {

  /**
   * Constructor
   * 
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public TraditionalStaticPathOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Retrieve an OD path matrix object for a specified mode
   * 
   * @param mode the specified mode
   * @return the OD path object
   */
  @Override
  public Optional<ODPathMatrix> getODPathMatrix(Mode mode) {
    return Optional.of(((TraditionalStaticAssignment) trafficAssignment).getIterationData().getODPathMatrix(mode));
  }

}