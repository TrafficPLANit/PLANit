package org.goplanit.network.layer.service;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdGroupingToken;
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
   * Based on network node
   * 
   * @return network node position
   */
  @Override
  public final Point getPosition() {
    LOGGER.warning("Unable to retrieve single position, use underlying physical link segment node positions instead");
    return null;
  }

  @Override
  public void setPosition(Point position) {
    LOGGER.warning("Unable to modify position, network node determines position of service node indirectly");
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
