package org.planit.network.virtual;

import org.planit.graph.EdgeSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.Connectoid;
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
    return IdGenerator.generateId(groupId, ConnectoidSegment.class);
  }

  /**
   * Set connectoid segment id
   * 
   * @param generateConnectoidSegmentId to set
   */
  protected void setConnectoidSegmentId(long connectoidSegmentId) {
    this.connectoidSegmentId = connectoidSegmentId;
  }

  /**
   * Constructor
   *
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param parentConnectoid parent connectoid
   * @param directionAb      direction of travel
   * @throws PlanItException thrown when error
   */
  protected ConnectoidSegmentImpl(final IdGroupingToken groupId, final Connectoid parentConnectoid, final boolean directionAb) throws PlanItException {
    super(groupId, parentConnectoid, directionAb);
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
