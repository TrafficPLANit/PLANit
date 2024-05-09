package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.adapter.OdOutputTypeAdapterImpl;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;

import java.util.Optional;

/**
 * Adapter providing access to the data of the static LTM class relevant for origin-destination outputs without exposing
 * the internals of the traffic assignment class itself
 * 
 * @author markr
 *
 */
public class StaticLtmOdOutputTypeAdapter extends OdOutputTypeAdapterImpl {

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtm getAssignment() {
    return (StaticLtm) super.getAssignment();
  }

  /**
   * Constructor
   *
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public StaticLtmOdOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
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
    return Optional.of(getAssignment().getIterationData().getSkimMatrixData().getOdSkimMatrix(odSkimOutputType, mode));
  }

}
