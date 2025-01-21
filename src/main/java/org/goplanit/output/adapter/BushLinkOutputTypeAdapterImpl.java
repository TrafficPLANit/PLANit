package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.output.adapter.traits.BushNetworkSegmentsOutputTypeAdapterTraitImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Top-level class which defines the common methods required by bush link output type adapters.
 * Specifically designed for the use in conjunction with bushes and their link segments.
 * <p>
 *   Currently no additional options compared to the regular network link output type adapter are supported hence
 *   this class only masks its untyped counterpart, when new option are to be made available implement them here.
 * </p>
 * 
 * @author markr
 *
 */
public class BushLinkOutputTypeAdapterImpl
    extends UntypedEdgeOutputTypeAdapterImpl<EdgeSegment> implements BushLinkOutputTypeAdapter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BushLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /** trait implementation */
  private final BushNetworkSegmentsOutputTypeAdapterTraitImpl bushSegmentsTrait;

  /**
   * Access to trait
   */
  protected BushNetworkSegmentsOutputTypeAdapterTraitImpl getTrait(){
    return bushSegmentsTrait;
  }

  /**
   * Constructor
   *
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   * @param bushSegmentsTrait trait to provide access to bushes on adapter
   */
  public BushLinkOutputTypeAdapterImpl(
          OutputType outputType,
          TrafficAssignment trafficAssignment,
          BushNetworkSegmentsOutputTypeAdapterTraitImpl bushSegmentsTrait) {
    super(outputType, trafficAssignment);
    this.bushSegmentsTrait = bushSegmentsTrait;
  }

  @Override
  public Optional<Long> getInfrastructureLayerIdForMode(Mode mode) {
    return Optional.empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntities<? extends EdgeSegment> getLinkSegmentsForLayer(long layerId) {
    // base implementation assumes bushes are consistent and build upon the regular network
    // if not this method needs to be overridden in derived class.
    var layer = getAssignment().getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(layerId);
    return layer.getLinkSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RootedBush<? extends DirectedVertex, ? extends EdgeSegment>[] getBushes() {
    return bushSegmentsTrait.getBushes();
  }
}
