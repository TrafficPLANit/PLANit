package org.goplanit.output.adapter;

import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.network.layer.physical.PhysicalLayer;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.network.layer.TransportLayer;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Abstract class which defines the common methods required by Link output type adapters that specifically pertain to networks that have adopted physical layers for their network
 * representation
 * 
 * @author gman6028
 *
 */
public abstract class PhysicalLinkOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements UntypedLinkOutputTypeAdapter<LinkSegment> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PhysicalLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public PhysicalLinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Provide access to the link segments container
   * 
   * @param infrastructureLayerId to use
   */
  @Override
  public GraphEntities<LinkSegment> getPhysicalLinkSegments(long infrastructureLayerId) {
    TransportLayer networkLayer = getAssignment().getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(infrastructureLayerId);
    if (networkLayer instanceof PhysicalLayer) {
      return ((PhysicalLayer) networkLayer).getLinkSegments();
    }
    LOGGER.warning(String.format("Cannot collect physical link segments from infrastructure layer %s, as it is not a physical network layer", networkLayer.getXmlId()));
    return null;
  }

}
