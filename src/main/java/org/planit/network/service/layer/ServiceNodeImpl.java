package org.planit.network.service.layer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.service.ServiceLeg;
import org.planit.utils.network.service.ServiceLegSegment;
import org.planit.utils.network.service.ServiceNode;

/**
 * A ServiceNode is used in a ServiceNetwork where it holds a reference to a DirectedNode of the ServiceNetworkLayer's underlying physical network layer. Each ServiceNode
 * represents a location where at least a single service exists, e.g. a stop location of a public service vehicle
 * 
 * @author markr
 *
 */
public class ServiceNodeImpl extends ExternalIdAbleImpl implements ServiceNode {

  /** generated UID */
  private static final long serialVersionUID = 3704157577170156850L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNodeImpl.class.getCanonicalName());

  /** underlying network node */
  protected Node networkNode;

  /**
   * legs of this service node
   */
  protected final Map<Long, ServiceLeg> legs = new TreeMap<Long, ServiceLeg>();

  /**
   * Entry leg segments which connect to this service node
   */
  protected final Set<ServiceLegSegment> entryLegSegments = new TreeSet<ServiceLegSegment>();

  /**
   * Exit leg segments which connect to this service node
   */
  protected final Set<ServiceLegSegment> exitLegSegments = new TreeSet<ServiceLegSegment>();

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
    super(Vertex.generateVertexId(tokenId));
    this.networkNode = networkNode;
  }

  /**
   * Constructor
   * 
   * @param tokenId     contiguous id generation within this group for instances of this class
   * @param networkNode referenced by this service node
   */
  protected ServiceNodeImpl(final IdGroupingToken tokenId) {
    this(tokenId, null);
  }

  /**
   * Copy constructor
   * 
   * @param serviceNode to copy
   */
  protected ServiceNodeImpl(final ServiceNodeImpl serviceNode) {
    super(serviceNode);
    this.networkNode = serviceNode.getNetworkLayerNode();
    legs.putAll(serviceNode.legs);
    entryLegSegments.addAll(serviceNode.getEntryLegSegments());
    exitLegSegments.addAll(serviceNode.getExitLegSegments());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeImpl clone() {
    return new ServiceNodeImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addEdgeSegment(EdgeSegment edgeSegment) {
    if (!(edgeSegment instanceof ServiceLegSegment)) {
      LOGGER.warning("Unable to add, provided EdgeSegment no instance of ServiceLegSegment");
      return false;
    }

    if (edgeSegment.getUpstreamVertex().getId() == getId()) {
      return exitLegSegments.add((ServiceLegSegment) edgeSegment);
    } else if (edgeSegment.getDownstreamVertex().getId() == getId()) {
      return entryLegSegments.add((ServiceLegSegment) edgeSegment);
    }
    LOGGER.warning(String.format("Service leg segment %s (id:%d) does not have this service node %s (%d) on either end", edgeSegment.getExternalId(), edgeSegment.getId(),
        getExternalId(), getId()));
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEntryEdgeSegment(EdgeSegment edgeSegment) {
    return entryLegSegments.remove((ServiceLegSegment) edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeExitEdgeSegment(EdgeSegment edgeSegment) {
    return exitLegSegments.remove(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getEntryEdgeSegments() {
    return Collections.unmodifiableSet(this.entryLegSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getExitEdgeSegments() {
    return Collections.unmodifiableSet(this.exitLegSegments);
  }

  /**
   * Not available on service node
   */
  @Override
  public void addInputProperty(String key, Object value) {
    LOGGER.warning("Input properties of service node linked to underlying network node, not allowed to add");
  }

  /**
   * Not available on service nodes
   * 
   * @return null
   */
  @Override
  public Object getInputProperty(String key) {
    LOGGER.warning("Input properties of service node linked to underlying network node, collect them from there instead");
    return null;
  }

  /**
   * Not possible, linked to underlying network node
   */
  @Override
  public void setPosition(Point position) {
    LOGGER.warning("Position of service node linked to underlying network node, unable to change position directly");
  }

  /**
   * Based on network node
   * 
   * @return network node position
   */
  @Override
  public Point getPosition() {
    return networkNode.getPosition();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addEdge(Edge edge) {
    return legs.put(edge.getId(), (ServiceLeg) edge) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEdge(long edgeId) {
    return legs.remove(edgeId) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<ServiceLeg> getEdges() {
    return Collections.unmodifiableCollection(legs.values());
  }

  /**
   * Collect the network layer node this service node relates to
   * 
   * @return related network layer node
   */
  public Node getNetworkLayerNode() {
    return networkNode;
  }

}
