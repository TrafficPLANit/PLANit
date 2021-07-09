package org.planit.network.virtual;

import org.planit.graph.directed.EdgeSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.ConnectoidEdge;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * The link segment that connects a zone to the physical network is not a physical link segment. However in order to be able to efficiently conduct path searches this connection
 * needs to materialise in a similar form.
 *
 * to do this we construct ConnectoidLinkSegment instances which are a link segment, but do not have any physical characteristics apart from connecting a zone (via its centroid) to
 * a physical node.
 *
 * these segments are NOT registered on the network because they are not part of the physical network, instead they are registered on the adopted zoning. they are however
 * injected/connected to the connectoid reference nodes in the network as link segments to provide the above mentioned interface
 *
 * @author markr
 *
 */
public class ConnectoidSegmentImpl extends EdgeSegmentImpl implements ConnectoidSegment {

  /** generated UID */
  private static final long serialVersionUID = 6462304338451088764L;

  /**
   * unique internal identifier
   */
  private long connectoidSegmentId;

  /**
   * generate unique connectoid segment id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return linkSegmentId
   */
  protected static long generateConnectoidSegmentId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, ConnectoidSegment.CONNECTOID_SEGMENT_ID_CLASS);
  }

  /**
   * Set connectoid segment id
   * 
   * @param connectoidSegmentId to set
   */
  protected void setConnectoidSegmentId(long connectoidSegmentId) {
    this.connectoidSegmentId = connectoidSegmentId;
  }

  /**
   * recreate the internal connectoid segment id and set it
   * 
   * @return updated id
   */
  protected long recreateConnectoidSegmentId(IdGroupingToken tokenId) {
    long newConnectoidSegmentId = generateConnectoidSegmentId(tokenId);
    setConnectoidSegmentId(newConnectoidSegmentId);
    return newConnectoidSegmentId;
  }

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentEdge  parent connectoid
   * @param directionAb direction of travel
   * @throws PlanItException thrown when error
   */
  protected ConnectoidSegmentImpl(final IdGroupingToken groupId, final ConnectoidEdge parentEdge, final boolean directionAb) throws PlanItException {
    super(groupId, parentEdge, directionAb);
    setConnectoidSegmentId(generateConnectoidSegmentId(groupId));
  }

  /**
   * Copy constructor
   * 
   * @param connectoidSegmentImpl to set
   */
  protected ConnectoidSegmentImpl(ConnectoidSegmentImpl connectoidSegmentImpl) {
    super(connectoidSegmentImpl);
    setConnectoidSegmentId(connectoidSegmentImpl.getConnectoidSegmentId());
  }

  // Public getters - setters

  /**
   * Recreate internal ids: id and connectoid segment id
   * 
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateConnectoidSegmentId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getConnectoidSegmentId() {
    return connectoidSegmentId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegmentImpl clone() {
    return new ConnectoidSegmentImpl(this);
  }
}
