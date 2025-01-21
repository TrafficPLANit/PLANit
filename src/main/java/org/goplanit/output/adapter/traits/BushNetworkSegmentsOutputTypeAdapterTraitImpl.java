package org.goplanit.output.adapter.traits;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

import java.util.logging.Logger;

/**
 * Top-level abstract class which implements the common methods/traits required by bush-based (link) segment
 * output type adapter.
 *
 * @author markr
 *
 */
public abstract class BushNetworkSegmentsOutputTypeAdapterTraitImpl
    extends UntypedNetworkSegmentsOutputTypeAdapterTraitImpl<EdgeSegment>
        implements UntypedBushSegmentsOutputTypeAdapterTrait<EdgeSegment> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER =
          Logger.getLogger(BushNetworkSegmentsOutputTypeAdapterTraitImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public BushNetworkSegmentsOutputTypeAdapterTraitImpl(TrafficAssignment trafficAssignment) {
    super(trafficAssignment);
  }

}
