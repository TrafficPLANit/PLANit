package org.goplanit.network.layer.service;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.network.ServiceNetwork;
import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.network.layer.modifier.ServiceNetworkLayerModifierImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceLegSegments;
import org.goplanit.utils.network.layer.service.ServiceLegs;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.network.layer.service.ServiceNodes;

/**
 * A service network layer is built on top of a physical network (layer). Its nodes (service nodes) and links (legs) contain references to the underlying physical network resulting
 * in an efficient use of memory while allowing one to treat the service layer as a normal network at the same time. Service Nodes are one-to-one matches to the underlying network
 * nodes and represent locations where services can be accessed/egressed. Edges are represented by ServiceLegs, while edge segments are represented by ServiceLegSegments. A leg
 * comprises one or more physical links in the underlying network and as long as the underlying links differ, multiple legs can exist between the same service nodes.
 * ServiceLegSegments have a direction which determines the order in which they traverse the underlying physical links of the ServiceLeg.
 *
 * @author markr
 *
 */
public class ServiceNetworkLayerImpl extends UntypedNetworkLayerImpl<ServiceNode, ServiceLeg, ServiceLegSegment> implements ServiceNetworkLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerImpl.class.getCanonicalName());

  /** the related physical layer */
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
   * @param tokenId to use for id generation of instances of this class
   */
  protected ServiceNetworkLayerImpl(final IdGroupingToken tokenId) {
    this(tokenId, null);
  }

  /**
   * Constructor. Instance only usable after user explicitly sets the parent network layer
   * 
   * @param tokenId     to use for id generation of instances of this class
   * @param parentLayer this service layer is built on top of this network (when null user is expected to set it manually afterwards)
   */
  protected ServiceNetworkLayerImpl(final IdGroupingToken tokenId, final MacroscopicNetworkLayer parentLayer) {
    this(tokenId, parentLayer, new ServiceNodesImpl(tokenId), new ServiceLegsImpl(tokenId), new ServiceLegSegmentsImpl(tokenId));
  }

  /**
   * Constructor
   * 
   * @param tokenId            to use for id generation of instances of this class
   * @param parentNetworkLayer this service layer is built on top of this network (whn null user is expected to set it manually afterwards)
   * @param nodes              to use
   * @param legs               to use
   * @param legSegments        to use
   */
  protected ServiceNetworkLayerImpl(final IdGroupingToken tokenId, final MacroscopicNetworkLayer parentNetworkLayer, final ServiceNodes nodes, final ServiceLegs legs,
                                    final ServiceLegSegments legSegments) {
    super(tokenId, nodes, legs, legSegments);
    this.layerModifier = new ServiceNetworkLayerModifierImpl<>(this, this.graph); // overwrite default from super <-- not pretty but otherwise no access to graph yet
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
    this.layerModifier = serviceNetworkLayerImpl.getLayerModifier();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ServiceLegs getLegs() {
    return (ServiceLegs) getGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ServiceLegSegments getLegSegments() {
    return (ServiceLegSegments) getGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ServiceNodes getServiceNodes() {
    return (ServiceNodes) getGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer getParentNetworkLayer() {
    return parentNetworkLayer;
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
  public ServiceNetworkLayerImpl clone() {
    return new ServiceNetworkLayerImpl(this);
  }

  /**
   * A service network does not allow for registering supported modes as the supported modes are defined by its parent network already. log warning and do nothing
   * 
   * @param supportedMode to register
   * @return false
   */
  @Override
  public boolean registerSupportedMode(Mode supportedMode) {
    LOGGER.warning(String.format("Unable to register additional supported modes on service network layer %s, do so on parent network layer %s instead", getXmlId(),
        getParentNetworkLayer().getXmlId()));
    return false;
  }

  /**
   * A service network does not allow for registering supported modes as the supported modes are defined by its parent network already. log warning and do nothing
   * 
   * @param supportedModes to register
   * @return always return false
   */
  @Override
  public boolean registerSupportedModes(Collection<Mode> supportedModes) {
    LOGGER.warning(String.format("Unable to register additional supported modes on service network layer %s, do so on parent network layer %s instead", getXmlId(),
        getParentNetworkLayer().getXmlId()));
    return false;
  }

  /**
   * Collect supported modes, obtained from parent layer
   * 
   * @return parent layer's supported modes
   */
  @Override
  public Collection<Mode> getSupportedModes() {
    return getParentNetworkLayer().getSupportedModes();
  }

  /**
   * Result delegate from underlying parent layer
   * 
   * @return parent layer's result on verifying support for a mode
   */
  @Override
  public boolean supports(Mode mode) {
    return getParentNetworkLayer().supports(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayerModifierImpl<ServiceNode, ServiceLeg, ServiceLegSegment> getLayerModifier(){
    return (ServiceNetworkLayerModifierImpl<ServiceNode, ServiceLeg, ServiceLegSegment>) super.getLayerModifier();
  }
}
