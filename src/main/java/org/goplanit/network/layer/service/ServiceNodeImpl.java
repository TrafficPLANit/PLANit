package org.goplanit.network.layer.service;

import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.locationtech.jts.geom.Point;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;

/**
 * A ServiceNode is used in a ServiceNetwork where it holds a reference to a DirectedNode of the ServiceNetworkLayer's underlying physical network layer. Each ServiceNode
 * represents a location where at least a single service exists, e.g. a stop location of a public service vehicle
 * 
 * @author markr
 *
 */
public class ServiceNodeImpl extends DirectedVertexImpl implements ServiceNode {

  /** generated UID */
  private static final long serialVersionUID = 3704157577170156850L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNodeImpl.class.getCanonicalName());

  /** underlying network node */
  protected Node networkNode;

  /**
   * Set the network layer node this service node refers to
   * 
   * @param networkNode to use
   */
  protected void setNetworkLayerNode(Node networkNode) {
    this.networkNode = networkNode;
  }

  /**
   * Constructor
   * 
   * @param tokenId     contiguous id generation within this group for instances of this class
   * @param networkNode referenced by this service node
   */
  protected ServiceNodeImpl(final IdGroupingToken tokenId, final Node networkNode) {
    super(tokenId);
    this.networkNode = networkNode;
  }

  /**
   * Copy constructor
   * 
   * @param serviceNode to copy
   */
  protected ServiceNodeImpl(final ServiceNodeImpl serviceNode) {
    super(serviceNode);
    this.networkNode = serviceNode.getParentNode();
    edges.putAll(serviceNode.edges);
    entryEdgeSegments.addAll(serviceNode.entryEdgeSegments);
    exitEdgeSegments.addAll(serviceNode.exitEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addEdgeSegment(EdgeSegment serviceLegSegment) {
    if (!(serviceLegSegment instanceof ServiceLegSegment)) {
      LOGGER.warning("Unable to add, provided EdgeSegment no instance of ServiceLegSegment");
      return false;
    }
    
    return super.addEdgeSegment(serviceLegSegment);
  }

  /**
   * Based on network node
   * 
   * @return network node position
   */
  @Override
  public final Point getPosition() {
    return networkNode.getPosition();
  }
  
  @Override  
  public void setPosition(Point position) {
    LOGGER.warning("Unable to modify position, network node determines position of service node indirectly");
  }

  /**
   * Collect the network layer node this service node relates to
   * 
   * @return related network layer node
   */
  @Override
  public final Node getParentNode() {
    return networkNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeImpl clone() {
    return new ServiceNodeImpl(this);
  }

}
