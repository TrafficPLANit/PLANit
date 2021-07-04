package org.planit.network.virtual;

import org.planit.graph.directed.DirectedEdgeImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.ConnectoidEdge;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Connectoid;

/**
 * Edge implementation that represent edges that exist between centroids and connectoids (their node reference), so not physical entities but rather virtual links
 * 
 * @author markr
 *
 */
public class ConnectoidEdgeImpl extends DirectedEdgeImpl implements ConnectoidEdge {

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
   * Constructor
   *
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param centroidA the centroid at one end of the connectoid
   * @param vertexB   the vertex at the other end of the connectoid
   * @param length    length of the current connectoid
   * @throws PlanItException thrown if there is an error
   */
  protected ConnectoidEdgeImpl(final IdGroupingToken groupId, final Centroid centroidA, final DirectedVertex vertexB, final double length) throws PlanItException {
    super(groupId, centroidA, vertexB, length);
    setConnectoidEdgeId(generateConnectoidEdgeId(groupId));
  }

  /**
   * Copy constructor
   * 
   * @param connectoidEdgeImpl to copy
   */
  protected ConnectoidEdgeImpl(ConnectoidEdgeImpl connectoidEdgeImpl) {
    super(connectoidEdgeImpl);
    setConnectoidEdgeId(connectoidEdgeImpl.getConnectoidEdgeId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegment registerConnectoidSegment(ConnectoidSegment connectoidSegment, boolean directionAB) throws PlanItException {
    return (ConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getConnectoidEdgeId() {
    return connectoidEdgeId;
  }

}
