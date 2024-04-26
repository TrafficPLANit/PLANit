package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.output.adapter.PathOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;

import java.util.Optional;

/**
 * Adapter providing access to the data of the StaticLtm class relevant for path outputs without exposing the internals of the traffic assignment class itself
 *
 * @author markr
 *
 */
public class StaticLtmPathOutputTypeAdapter extends PathOutputTypeAdapterImpl {

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
  public StaticLtmPathOutputTypeAdapter(final OutputType outputType, final TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Retrieve an OD path matrix object for a specified mode
   *
   * @param mode the specified mode
   * @return the OD path object
   */
  @Override
  public Optional<OdMultiPaths<?, ?>> getOdMultiPaths(Mode mode) {
    return Optional.of(((StaticLtmPathStrategy) getAssignment().getStrategy()).getOdMultiPaths(mode));
  }
}
