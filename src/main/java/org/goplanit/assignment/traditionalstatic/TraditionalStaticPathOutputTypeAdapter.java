package org.goplanit.assignment.traditionalstatic;

import java.util.Optional;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.output.adapter.PathOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for OD path outputs without exposing the internals of the traffic assignment class itself
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticPathOutputTypeAdapter extends PathOutputTypeAdapterImpl {

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
  public Optional<OdPathMatrix> getOdPathMatrix(Mode mode) {
    return Optional.of(getAssignment().getIterationData().getOdPathMatrix(mode));
  }

}
