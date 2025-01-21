package org.goplanit.output.adapter.traits;

import java.util.Optional;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * Top-level abstract class which implements the common methods/traits required by network based (link) segment
 * output type adapters
 * 
 * @author markr
 *
 */
public class MacroscopicNetworkSegmentsOutputTypeAdapterTraitImpl
    extends UntypedNetworkSegmentsOutputTypeAdapterTraitImpl<MacroscopicLinkSegment> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER =
          Logger.getLogger(MacroscopicNetworkSegmentsOutputTypeAdapterTraitImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public MacroscopicNetworkSegmentsOutputTypeAdapterTraitImpl(TrafficAssignment trafficAssignment) {
    super(trafficAssignment);
  }

  /**
   * Provide access to the macroscopic link segments
   *
   * @param layerId to use
   */
  @Override
  public MacroscopicLinkSegments getLinkSegmentsForLayer(long layerId) {
    NetworkLayer networkLayer =
            trafficAssignment.getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(layerId);
    if (networkLayer instanceof MacroscopicNetworkLayer) {
      return ((MacroscopicNetworkLayer) networkLayer).getLinkSegments();
    }
    LOGGER.warning(
            String.format("Cannot collect macroscopic physical link segments from infrastructure layer %s, as it is " +
                            "not a macroscopic physical network layer", networkLayer.getXmlId()));
    return null;
  }
}
