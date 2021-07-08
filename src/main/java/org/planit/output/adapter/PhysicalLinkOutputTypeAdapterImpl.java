package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.OutputType;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.PhysicalLayer;

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
    TransportLayer networkLayer = this.trafficAssignment.getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(infrastructureLayerId);
    if (networkLayer instanceof PhysicalLayer) {
      return ((PhysicalLayer) networkLayer).getLinkSegments();
    }
    LOGGER.warning(String.format("cannot collect physical link segments from infrastructure layer %s, as it is not a physical network layer", networkLayer.getXmlId()));
    return null;
  }

}
