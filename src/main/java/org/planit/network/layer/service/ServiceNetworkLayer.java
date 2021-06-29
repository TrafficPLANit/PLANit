package org.planit.network.layer.service;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.DirectedGraphImpl;
import org.planit.network.layer.TopologicalLayerImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TopologicalLayer;
import org.planit.utils.network.layer.physical.PhysicalLayer;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.network.layer.service.ServiceLegSegments;
import org.planit.utils.network.layer.service.ServiceLegs;
import org.planit.utils.network.layer.service.ServiceNetworkLayerBuilder;
import org.planit.utils.network.layer.service.ServiceNode;
import org.planit.utils.network.layer.service.ServiceNodes;

/**
 * A service network layer is built on top of a physical network (layer). Its nodes (service nodes) and links (legs) contain references to the underlying network resulting in an
 * efficient use of memory while allowing one to treat the service layer as a normal network at the same time. Service Nodes are one-to-one matches to the underlying network nodes
 * and represent locations where services exist. Edges are represented by ServiceLegs, while edge segments are represented by ServiceLegSegments. A leg comprises of one or more
 * physical links in the underlying network and as long as the underlying links differ multiple legs can exist between service nodes. ServiceLegSegments have a direction in which
 * order they traverse the underlying links of the ServiceLeg.
 *
 * @author markr
 *
 */
public class ServiceNetworkLayer extends TopologicalLayerImpl implements TopologicalLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayer.class.getCanonicalName());

  /**
   * the network builder
   */
  private final ServiceNetworkLayerBuilder serviceNetworkLayerBuilder;

  /**
   * The graph containing the nodes, links, and link segments (or derived implementations)
   */
  private final DirectedGraph<ServiceNode, ServiceLeg, ServiceLegSegment> graph;

  /** The physical network this service network is built on top of */
  @SuppressWarnings("unused")
  private PhysicalLayer<?, ?, ?> parentNetworkLayer;

  /**
   * The graph this service network is build upon
   * 
   * @return graph used
   */
  protected DirectedGraph<ServiceNode, ServiceLeg, ServiceLegSegment> getGraph() {
    return this.graph;
  }

  /**
   * The parent network layer this service network layer is to be build upon
   * 
   * @param parentNetworkLayer to use
   */
  protected void setParentNetworkLayer(final PhysicalLayer<?, ?, ?> parentNetworkLayer) {
    this.parentNetworkLayer = parentNetworkLayer;
  }

  /**
   * Constructor. Instance only usable after user explicitly sets the parent network layer
   * 
   * @param tokenId                    to use for id generation of instances of this class
   * @param serviceNetworkLayerBuilder this is the builder to use for this layer
   */
  public ServiceNetworkLayer(final IdGroupingToken tokenId, final ServiceNetworkLayerBuilder serviceNetworkLayerBuilder) {
    this(tokenId, null, serviceNetworkLayerBuilder);
  }

  /**
   * Constructor
   * 
   * @param tokenId                    to use for id generation of instances of this class
   * @param parentNetworkLayer         this service layer is built on top of this network (whn null user is expected to set it manually afterwards)
   * @param serviceNetworkLayerBuilder this is the builder to use for this layer
   */
  protected ServiceNetworkLayer(final IdGroupingToken tokenId, final PhysicalLayer<?, ?, ?> parentNetworkLayer, final ServiceNetworkLayerBuilder serviceNetworkLayerBuilder) {
    super(tokenId);
    this.parentNetworkLayer = parentNetworkLayer;
    this.serviceNetworkLayerBuilder = serviceNetworkLayerBuilder;

    this.graph = new DirectedGraphImpl<ServiceNode, ServiceLeg, ServiceLegSegment>(tokenId, this.serviceNetworkLayerBuilder /* for graph builder part */);

    this.serviceNodes = new ServiceNodesImpl<ServiceNode>(getGraph().getVertices());
    this.serviceLegs = new ServiceLegsImpl<ServiceLeg>(getGraph().getEdges());
    this.serviceLegSegments = new ServiceLegSegmentsImpl<ServiceLegSegment>(getGraph().getEdgeSegments());
  }

  // PUBLIC

  /**
   * class instance containing all service leg specific functionality
   */
  public final ServiceLegs<ServiceLeg> serviceLegs;

  /**
   * Alternative to using the service legs public member
   * 
   * @return the service legs
   */
  public final ServiceLegs<ServiceLeg> getServiceLegs() {
    return serviceLegs;
  }

  /**
   * class instance containing all service leg segment specific functionality
   */
  public final ServiceLegSegments<ServiceLegSegment> serviceLegSegments;

  /**
   * Alternative to using the serviceLegSegments public member
   * 
   * @return the serviceLegSegments
   */
  public final ServiceLegSegments<ServiceLegSegment> getServiceLegSegments() {
    return serviceLegSegments;
  }

  /**
   * class instance containing all nodes specific functionality
   */
  public final ServiceNodes<ServiceNode> serviceNodes;

  /**
   * alternative to using the nodes public member
   * 
   * @return the nodes
   */
  public final ServiceNodes<ServiceNode> getServiceNodes() {
    return serviceNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return graph.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix) {
    /* log supported modes */
    LOGGER.info(String.format("%s#supported modes: %s", prefix, getSupportedModes().stream().map((mode) -> {
      return mode.getXmlId();
    }).collect(Collectors.joining(", "))));

    /* log infrastructure components */
    LOGGER.info(String.format("%s#service legs: %d", prefix, serviceLegs.size()));
    LOGGER.info(String.format("%s#service leg segments: %d", prefix, serviceLegSegments.size()));
    LOGGER.info(String.format("%s#service nodes: %d", prefix, serviceNodes.size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean validate() {
    boolean isValid = graph.validate();
    for (ServiceLeg leg : serviceLegs) {
      isValid = isValid && leg.validate();
    }
    for (ServiceLegSegment legSegment : serviceLegSegments) {
      isValid = isValid && legSegment.validate();
    }
    for (ServiceNode node : serviceNodes) {
      isValid = isValid && node.validate();
    }
    return isValid;
  }

  /**
   * No transformation possible on service network directly, transform underlying physical network instead, warning issues
   */
  @Override
  public void transform(CoordinateReferenceSystem fromCoordinateReferenceSystem, CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException {
    LOGGER.warning("Service network crs cannot be transformed directly, transofrmations are to be conducted on underlying physical network layer instead");
  }

  @Override
  public void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, Set<RemoveSubGraphListener<?, ?>> listeners) throws PlanItException {
    // TODO: all possible network modifications require each service layer to register itself onto the network for providing handlers to deal with any changes in the
    // physical network that affect the service layer
    LOGGER.warning("NOT YET IMPLEMENTED ON SERVICE NETWORK");
  }

}
