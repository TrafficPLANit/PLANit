package org.planit.network.layer;

import java.util.Set;
import java.util.logging.Logger;

import org.planit.network.layer.service.ServiceLegSegmentsImpl;
import org.planit.network.layer.service.ServiceLegsImpl;
import org.planit.network.layer.service.ServiceNodesImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.network.layer.service.ServiceLegSegments;
import org.planit.utils.network.layer.service.ServiceLegs;
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
public class ServiceNetworkLayerImpl extends UntypedDirectedGraphLayerImpl<ServiceNode, ServiceNodes, ServiceLeg, ServiceLegs, ServiceLegSegment, ServiceLegSegments>
    implements ServiceNetworkLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerImpl.class.getCanonicalName());

  /** the parent layer */
  protected MacroscopicNetworkLayer parentNetworkLayer;

  /**
   * The parent network layer this service network layer is to be build upon
   * 
   * @param parentNetworkLayer to use
   */
  protected void setParentNetworkLayer(final MacroscopicNetworkLayer parentNetworkLayer) {
    this.parentNetworkLayer = parentNetworkLayer;
  }

  /**
   * Constructor. Instance only usable after user explicitly sets the parent network layer
   * 
   * @param tokenId                    to use for id generation of instances of this class
   * @param serviceNetworkLayerBuilder this is the builder to use for this layer
   */
  protected ServiceNetworkLayerImpl(final IdGroupingToken tokenId) {
    this(tokenId, null);
  }

  /**
   * Constructor. Instance only usable after user explicitly sets the parent network layer
   * 
   * @param tokenId                    to use for id generation of instances of this class
   * @param parentNetworkLayer         this service layer is built on top of this network (when null user is expected to set it manually afterwards)
   * @param serviceNetworkLayerBuilder this is the builder to use for this layer
   */
  protected ServiceNetworkLayerImpl(final IdGroupingToken tokenId, final MacroscopicNetworkLayer parentLayer) {
    this(tokenId, parentLayer, new ServiceNodesImpl(tokenId), new ServiceLegsImpl(tokenId), new ServiceLegSegmentsImpl(tokenId));
  }

  /**
   * Constructor
   * 
   * @param tokenId                    to use for id generation of instances of this class
   * @param parentNetworkLayer         this service layer is built on top of this network (whn null user is expected to set it manually afterwards)
   * @param serviceNetworkLayerBuilder this is the builder to use for this layer
   */
  protected ServiceNetworkLayerImpl(final IdGroupingToken tokenId, final MacroscopicNetworkLayer parentNetworkLayer, final ServiceNodes nodes, final ServiceLegs legs,
      final ServiceLegSegments legSegments) {
    super(tokenId, nodes, legs, legSegments);
    this.parentNetworkLayer = parentNetworkLayer;
  }

  // PUBLIC

  /**
   * Copy constructor
   * 
   * @param serviceNetworkLayerImpl to copy
   */
  public ServiceNetworkLayerImpl(ServiceNetworkLayerImpl serviceNetworkLayerImpl) {
    super(serviceNetworkLayerImpl);
    this.parentNetworkLayer = serviceNetworkLayerImpl.parentNetworkLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ServiceLegs getLegs() {
    return getGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ServiceLegSegments getLegSegments() {
    return getGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ServiceNodes getServiceNodes() {
    return getGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix) {
    super.logInfo(prefix);

    /* log infrastructure components */
    LOGGER.info(String.format("%s#service legs: %d", prefix, getLegs().size()));
    LOGGER.info(String.format("%s#service leg segments: %d", prefix, getLegSegments().size()));
    LOGGER.info(String.format("%s#service nodes: %d", prefix, getServiceNodes().size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, Set<RemoveSubGraphListener> listeners) throws PlanItException {
    // TODO: all possible network modifications require each service layer to register itself onto the network for providing handlers to deal with any changes in the
    // physical network that affect the service layer
    LOGGER.warning("NOT YET IMPLEMENTED ON SERVICE NETWORK");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayerImpl clone() {
    return new ServiceNetworkLayerImpl(this);
  }

}
