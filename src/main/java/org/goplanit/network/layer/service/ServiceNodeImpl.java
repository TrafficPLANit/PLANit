package org.goplanit.network.layer.service;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.misc.IteratorUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.locationtech.jts.geom.Point;

/**
 * A ServiceNode is used in a ServiceNetwork where it holds a reference to a DirectedNode of the ServiceNetworkLayer's underlying physical network layer. Each ServiceNode
 * represents a location where at least a single service exists, e.g. a stop location of a public service vehicle
 * 
 * @author markr
 *
 */
public class ServiceNodeImpl extends DirectedVertexImpl<ServiceLegSegment> implements ServiceNode {

  /** generated UID */
  private static final long serialVersionUID = 3704157577170156850L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNodeImpl.class.getCanonicalName());

  /**
   * Collect stream of downstream physical nodes of attached entry service leg segments (if any)
   *
   * @return the stream
   */
  protected Stream<Node> getEntrySegmentsDownstreamPhysicalNodeStream(){
    return IteratorUtils.asStream(getEntryEdgeSegments().iterator()).filter( e -> e.hasPhysicalParentSegments()).map(
        e -> ListUtils.getLastValue(e.getPhysicalParentSegments()).getDownstreamNode());
  }

  /**
   * Collect stream of upstream physical nodes of attached exit service leg segments (if any)
   *
   * @return the stream
   */
  protected Stream<Node> getExitSegmentsUpstreamPhysicalNodeStream(){
    return IteratorUtils.asStream(getExitEdgeSegments().iterator()).filter( e -> e.hasPhysicalParentSegments()).map(
        e -> ListUtils.getFirstValue(e.getPhysicalParentSegments()).getUpstreamNode());
  }

  /**
   * Constructor
   * 
   * @param tokenId     contiguous id generation within this group for instances of this class
   */
  protected ServiceNodeImpl(final IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ServiceNodeImpl(final ServiceNodeImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Construct new position based on underlying physical node(s). If multiple physical nodes are used to represent
   * this service node, the average position of each of them is returned. Unlike physical nodes, the position of
   * service nodes does not live on the object instance, it is recreated every time the position is queried.
   * 
   * @return newly inferred service node position
   */
  @Override
  public final Point getPosition() {
    var physicalParentNodes = getPhysicalParentNodes();
    if(physicalParentNodes.isEmpty()){
      LOGGER.warning(String.format("No physical parent nodes available on service node (%s), position unknown", getIdsAsString()));
      return null;
    }

    if(physicalParentNodes.size() == 1) {
      var parentNode = physicalParentNodes.iterator().next();
      if(parentNode.hasPosition()) {
        return (Point) physicalParentNodes.iterator().next().getPosition().copy();
      }
      return null;
    }

    /* multiple locations, construct average position */
    var averageX = physicalParentNodes.stream().filter(n -> n.hasPosition()).collect(Collectors.averagingDouble( n -> n.getPosition().getCoordinate().x));
    var averageY = physicalParentNodes.stream().filter(n -> n.hasPosition()).collect(Collectors.averagingDouble( n -> n.getPosition().getCoordinate().y));
    return PlanitJtsUtils.createPoint(averageX, averageY);
  }

  @Override
  public void setPosition(Point position) {
    LOGGER.warning("Unable to modify position, physical network node indirectly determines position of service node");
  }

  /**
   * Collect the physical nodes at the extremities of all underlying physical link segments. Since these can be different, these might result
   * in multiple physical nodes rather than one
   * 
   * @return related network layer node(s)
   */
  @Override
  public final Set<Node> getPhysicalParentNodes() {
    return Stream.concat(getExitSegmentsUpstreamPhysicalNodeStream(), getEntrySegmentsDownstreamPhysicalNodeStream()).collect(Collectors.toSet());
  }

  /*
   * {@inheritDoc
   */
  @Override
  public boolean hasPhysicalParentNodes() {
    return  getExitSegmentsUpstreamPhysicalNodeStream().findFirst().isPresent() || getEntrySegmentsDownstreamPhysicalNodeStream().findFirst().isPresent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeImpl shallowClone() {
    return new ServiceNodeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeImpl deepClone() {
    return new ServiceNodeImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isMappedToPhysicalParentNode(Node physicalParentNode) {
    boolean match = getExitSegmentsUpstreamPhysicalNodeStream().anyMatch(e -> e.equals(physicalParentNode));
    if(match){
      return true;
    }
    return getEntrySegmentsDownstreamPhysicalNodeStream().anyMatch(e -> e.equals(physicalParentNode));
  }

}
