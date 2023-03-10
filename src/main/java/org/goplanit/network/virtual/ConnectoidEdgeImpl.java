package org.goplanit.network.virtual;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.Connectoid;

/**
 * Edge implementation that represent edges that exist between centroids and connectoids (their node reference), so not physical entities but rather virtual links
 * 
 * @author markr
 *
 */
public class ConnectoidEdgeImpl extends DirectedEdgeImpl<DirectedVertex, EdgeSegment> implements ConnectoidEdge {

  /**
   * generated serial version id
   */
  private static final long serialVersionUID = 1212317697383702580L;

  /**
   * unique internal identifier across connectoid edges
   */
  protected long connectoidEdgeId;

  /**
   * Generate connectoid id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return id of connectoid edge
   */
  protected static long generateConnectoidEdgeId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Connectoid.class);
  }

  /**
   * Set the connectoidEdgeId
   * 
   * @param connectoidEdgeId to set
   */
  protected void setConnectoidEdgeId(long connectoidEdgeId) {
    this.connectoidEdgeId = connectoidEdgeId;
  }

  /**
   * recreate the internal connectoid edge id and set it
   * 
   * @param tokenId to use
   * @return updated id
   */
  protected long recreateConnectoidEdgeId(IdGroupingToken tokenId) {
    long newConnectoidEdgeId = generateConnectoidEdgeId(tokenId);
    setConnectoidEdgeId(newConnectoidEdgeId);
    return newConnectoidEdgeId;
  }

  /**
   * Constructor
   *
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param centroidA the centroidVertex at one end of the connectoid
   * @param vertexB   the vertex at the other end of the connectoid
   * @param length    length of the current connectoid
   */
  protected ConnectoidEdgeImpl(final IdGroupingToken groupId, final CentroidVertex centroidA, final DirectedVertex vertexB, final double length) {
    super(groupId, centroidA, vertexB, length);
    setConnectoidEdgeId(generateConnectoidEdgeId(groupId));
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConnectoidEdgeImpl(ConnectoidEdgeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setConnectoidEdgeId(other.getConnectoidEdgeId());
  }

  /**
   * Recreate internal ids: id and connectoid edge id
   * 
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateConnectoidEdgeId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegment registerConnectoidSegment(ConnectoidSegment connectoidSegment, boolean directionAB) {
    return (ConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getConnectoidEdgeId() {
    return connectoidEdgeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdgeImpl shallowClone() {
    return new ConnectoidEdgeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdgeImpl deepClone() {
    return new ConnectoidEdgeImpl(this, true);
  }

}
