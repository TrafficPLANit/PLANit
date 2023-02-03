package org.goplanit.network.layer.physical;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;

/**
 * Conjugate Link segment object representing conjugate of original network's adjacent link segment pair, i.e. turn
 *
 * @author markr
 *
 */
public class ConjugateLinkSegmentImpl extends EdgeSegmentImpl<ConjugateLink> implements ConjugateLinkSegment {

  /** UID */
  private static final long serialVersionUID = -2965215852323364946L;

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   */
  protected ConjugateLinkSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) {
    this(groupId, null, directionAB);
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parent      parent link of segment
   * @param directionAb direction of travel
   */
  protected ConjugateLinkSegmentImpl(final IdGroupingToken groupId, final ConjugateLink parent, final boolean directionAb) {
    super(groupId, parent, directionAb);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  protected ConjugateLinkSegmentImpl(ConjugateLinkSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentImpl shallowClone() {
    return new ConjugateLinkSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentImpl deepClone() {
    return new ConjugateLinkSegmentImpl(this, true);
  }

}
