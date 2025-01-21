package org.goplanit.output.adapter.traits;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Top-level default implementation class which implements the common methods/traits assuming a simple network based
 * approach.
 *
 * @author markr
 *
 */
public class UntypedNetworkSegmentsOutputTypeAdapterTraitImpl<ES extends EdgeSegment>
    implements NetworkSegmentsOutputTypeAdapterTrait<ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER =
          Logger.getLogger(UntypedNetworkSegmentsOutputTypeAdapterTraitImpl.class.getCanonicalName());

  /**
   * the traffic assignment this trait is drawing from
   */
  protected final TrafficAssignment trafficAssignment;

  /**
   * Constructor
   *
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public UntypedNetworkSegmentsOutputTypeAdapterTraitImpl(TrafficAssignment trafficAssignment) {
    this.trafficAssignment = trafficAssignment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Long> getInfrastructureLayerIdForMode(Mode mode) {
    NetworkLayer networkLayer = trafficAssignment.getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode);
    return Optional.of(networkLayer != null ? networkLayer.getId() : null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntities<? extends ES> getLinkSegmentsForLayer(long layerId) {
    var networkLayer =
            trafficAssignment.getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(layerId);
    return (GraphEntities<? extends ES>) networkLayer.getLinkSegments();
  }
}
