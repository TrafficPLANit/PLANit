package org.goplanit.assignment.traditionalstatic;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.od.path.OdPath2MultiPathWrapper;
import org.goplanit.output.adapter.PathOutputTypeAdapterImpl;
import org.goplanit.output.adapter.SimulationOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for simulation outputs without
 * exposing the internals of the traffic assignment class itself
 * 
 * @author markr
 *
 */
public class TraditionalStaticSimulationOutputTypeAdapter extends SimulationOutputTypeAdapterImpl {

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
  public TraditionalStaticSimulationOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

}
