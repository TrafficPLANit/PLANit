package org.goplanit.network.virtual;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdge;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegment;

/**
 * Conjugate connectoid segment object representing conjugate of original network's adjacent connectoid
 * segment pair, i.e. turn from or to a centroid with one of the original links being null
 *
 * @author markr
 *
 */
public class ConjugateConnectoidSegmentImpl extends
        EdgeSegmentImpl<ConjugateConnectoidEdge> implements ConjugateConnectoidSegment {


  /**
   * unique internal identifier
   */
  private long conjugateConnectoidSegmentId;

  /**
   * generate unique conjugate connectoid segment id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return linkSegmentId
   */
  protected static long generateConjugateConnectoidSegmentId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, ConnectoidSegment.CONNECTOID_SEGMENT_ID_CLASS);
  }

  /**
   * Set conjugate connectoid segment id
   *
   * @param connectoidSegmentId to set
   */
  protected void setConjugateConnectoidSegmentId(long connectoidSegmentId) {
    this.conjugateConnectoidSegmentId = connectoidSegmentId;
  }

  /**
   * recreate the internal connectoid segment id and set it
   *
   * @param tokenId to use
   * @return updated id
   */
  protected long recreateConjugateConnectoidSegmentId(IdGroupingToken tokenId) {
    long newConnectoidSegmentId = generateConjugateConnectoidSegmentId(tokenId);
    setConjugateConnectoidSegmentId(newConnectoidSegmentId);
    return newConnectoidSegmentId;
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parent      parent conjugate connectoid edge of segment
   * @param directionAb direction of travel
   */
  protected ConjugateConnectoidSegmentImpl(
      final IdGroupingToken groupId, final ConjugateConnectoidEdge parent, final boolean directionAb) {
    super(groupId, parent, directionAb);
    setConjugateConnectoidSegmentId(recreateConjugateConnectoidSegmentId(groupId));
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateConnectoidSegmentImpl(ConjugateConnectoidSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setConjugateConnectoidSegmentId(other.getConnectoidSegmentId());
  }

  @Override
  public long getConnectoidSegmentId() {
    return conjugateConnectoidSegmentId;
  }

  /**
   * Recreate internal ids: id and connectoid segment id
   *
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateConjugateConnectoidSegmentId(tokenId);
    return super.recreateManagedIds(tokenId);
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
