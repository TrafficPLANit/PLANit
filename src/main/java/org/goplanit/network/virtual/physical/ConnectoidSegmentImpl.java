package org.goplanit.network.virtual.physical;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.network.layer.physical.LinkSegmentImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

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
public class ConnectoidSegmentImpl extends LinkSegmentImpl implements ConnectoidSegment {

  /** generated UID */
  private static final long serialVersionUID = 6462304338451088764L;

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentLink  parent
   * @param directionAb direction of travel
   */
  protected ConnectoidSegmentImpl(final IdGroupingToken groupId, final ConnectoidLink parentLink, final boolean directionAb) {
    super(groupId, parentLink, directionAb);
  }

  /**
   * Copy constructor
   * 
   * @param other to set
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConnectoidSegmentImpl(ConnectoidSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLink getParent(){
    return (ConnectoidLink) super.getParent();
  }

  // Public getters - setters

  /**
   * Recreate internal ids: id and connectoid segment id
   * 
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    return super.recreateManagedIds(tokenId);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegmentImpl shallowClone() {
    return new ConnectoidSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegmentImpl deepClone() {
    return new ConnectoidSegmentImpl(this, true);
  }

}
