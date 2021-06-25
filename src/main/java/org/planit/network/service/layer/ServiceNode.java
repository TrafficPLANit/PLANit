package org.planit.network.service.layer;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;

/**
 * A ServiceNode is used in a ServiceNetwork where it holds a reference to a DirectedNode of the ServiceNetworkLayer's underlying physical network layer. Each ServiceNode
 * represents a location where at least a single service exists, e.g. a stop location of a public service vehicle
 * 
 * @author markr
 *
 */
public class ServiceNode extends ExternalIdAbleImpl implements DirectedVertex {

  /** generated UID */
  private static final long serialVersionUID = 3704157577170156850L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNode.class.getCanonicalName());

  /** underlying network node */
  protected final Node networkNode;

  /**
   * legs of this service node
   */
  protected final Map<Long, ServiceLeg> edges = new TreeMap<Long, ServiceLeg>();

  /**
   * Entry leg segments which connect to this service node
   */
  protected final Set<ServiceLegSegment> entryEdgeSegments = new TreeSet<ServiceLegSegment>();

  /**
   * Exit leg segments which connect to this service node
   */
  protected final Set<ServiceLegSegment> exitEdgeSegments = new TreeSet<ServiceLegSegment>();

  /**
   * generate unique node id
   *
   * @param tokenId, contiguous id generation within this group for instances of this class
   * @return service node id
   */
  protected static long generateVertexId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, ServiceNode.class);
  }

  /**
   * Constructor
   * 
   * @param tokenId     contiguous id generation within this group for instances of this class
   * @param networkNode referenced by this service node
   */
  protected ServiceNode(final IdGroupingToken tokenId, final Node networkNode) {
    super(generateVertexId(tokenId));
    this.networkNode = networkNode;
  }

  /**
   * Copy constructor
   * 
   * @param serviceNode to copy
   */
  protected ServiceNode(ServiceNode serviceNode) {
    super(serviceNode);
    this.networkNode = serviceNode.getNetworkLayerNode();
    edges.putAll(serviceNode.edges);
    entryEdgeSegments.addAll(serviceNode.getEntryEdgeSegments());
    exitEdgeSegments.addAll(serviceNode.getExitEdgeSegments());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNode clone() {
    return new ServiceNode(this);
  }

  @Override
  public boolean addEdgeSegment(EdgeSegment edgeSegment) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeEntryEdgeSegment(EdgeSegment edgeSegment) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeExitEdgeSegment(EdgeSegment edgeSegment) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Set<EdgeSegment> getEntryEdgeSegments() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<EdgeSegment> getExitEdgeSegments() {
    // TODO Auto-generated method stub
    return null;
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

  @Override
  public boolean addEdge(Edge edge) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeEdge(Edge edge) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeEdge(long edgeId) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Collection<ServiceLeg> getEdges() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<Edge> getEdges(Vertex otherVertex) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean validate() {
    // TODO Auto-generated method stub
    return false;
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
