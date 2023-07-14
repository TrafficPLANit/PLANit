package org.goplanit.network.virtual;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdge;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegment;

/**
 * Conjugate connectoid segment object representing conjugate of original network's adjacent connectoid segment pair, i.e. turn from or to a centroid with one of the original links being null
 *
 * @author markr
 *
 */
public class ConjugateConnectoidSegmentImpl extends EdgeSegmentImpl<ConjugateConnectoidEdge> implements ConjugateConnectoidSegment {

  /** UID */
  private static final long serialVersionUID = -2965215852323364946L;

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parent      parent conjugate connectoid edge of segment
   * @param directionAb direction of travel
   */
  protected ConjugateConnectoidSegmentImpl(final IdGroupingToken groupId, final ConjugateConnectoidEdge parent, final boolean directionAb) {
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
