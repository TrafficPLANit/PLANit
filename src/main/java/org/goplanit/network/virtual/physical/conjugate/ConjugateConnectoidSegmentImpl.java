package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.network.layer.physical.LinkSegmentImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLink;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegment;

/**
 * Conjugate connectoid segment object representing conjugate of original network's adjacent connectoid
 * segment pair, i.e. turn from or to a centroid with one of the original links being null
 *
 * @author markr
 *
 */
public class ConjugateConnectoidSegmentImpl extends
    LinkSegmentImpl implements ConjugateConnectoidSegment {

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parent      parent conjugate connectoid edge of segment
   * @param directionAb direction of travel
   */
  protected ConjugateConnectoidSegmentImpl(
      final IdGroupingToken groupId, final ConjugateConnectoidLink parent, final boolean directionAb) {
    super(groupId, parent, directionAb);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateConnectoidSegmentImpl(ConjugateConnectoidSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode getDownstreamVertex() {
    return (ConjugateConnectoidNode) super.getDownstreamVertex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode getUpstreamVertex() {
    return (ConjugateConnectoidNode) super.getUpstreamVertex();
  }

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
  public ConjugateConnectoidLink getParent(){
    return (ConjugateConnectoidLink) super.getParent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegmentImpl shallowClone() {
    return new ConjugateConnectoidSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegmentImpl deepClone() {
    return new ConjugateConnectoidSegmentImpl(this, true);
  }

}
