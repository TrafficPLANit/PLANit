package org.planit.assignment.traditionalstatic;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.od.skim.OdSkimMatrix;
import org.planit.output.adapter.ODOutputTypeAdapterImpl;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.utils.mode.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for origin-destination outputs without exposing the internals of the traffic assignment
 * class itself
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignmentODOutputTypeAdapter extends ODOutputTypeAdapterImpl {

  /**
   * {@inheritDoc}
   */
  @Override
  protected TraditionalStaticAssignment getAssignment() {
    return (TraditionalStaticAssignment) super.getAssignment();
  }

  /**
   * Constructor
   * 
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public TraditionalStaticAssignmentODOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Retrieve an OD skim matrix for a specified OD skim output type and mode
   * 
   * @param odSkimOutputType the specified OD skim output type
   * @param mode             the specified mode
   * @return the OD skim matrix
   */
  public Optional<OdSkimMatrix> getODSkimMatrix(ODSkimSubOutputType odSkimOutputType, Mode mode) {
    return Optional.of(getAssignment().getIterationData().getODSkimMatrix(odSkimOutputType, mode));
  }

}
