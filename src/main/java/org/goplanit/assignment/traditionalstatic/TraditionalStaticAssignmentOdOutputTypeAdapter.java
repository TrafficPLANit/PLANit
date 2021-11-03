package org.goplanit.assignment.traditionalstatic;

import java.util.Optional;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.adapter.OdOutputTypeAdapterImpl;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for origin-destination outputs without exposing the internals of the traffic assignment
 * class itself
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignmentOdOutputTypeAdapter extends OdOutputTypeAdapterImpl {

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
  public TraditionalStaticAssignmentOdOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Retrieve an OD skim matrix for a specified OD skim output type and mode
   * 
   * @param odSkimOutputType the specified OD skim output type
   * @param mode             the specified mode
   * @return the OD skim matrix
   */
  public Optional<OdSkimMatrix> getOdSkimMatrix(OdSkimSubOutputType odSkimOutputType, Mode mode) {
    return Optional.of(getAssignment().getIterationData().getOdSkimMatrix(odSkimOutputType, mode));
  }

}
