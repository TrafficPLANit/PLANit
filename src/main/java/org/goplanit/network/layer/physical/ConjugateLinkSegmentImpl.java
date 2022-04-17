package org.goplanit.network.layer.physical;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;

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
   * @throws PlanItException throw when error
   */
  protected ConjugateLinkSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) throws PlanItException {
    this(groupId, null, directionAB);
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parent      parent link of segment
   * @param directionAB direction of travel
   * @throws PlanItException throw when error
   */
  protected ConjugateLinkSegmentImpl(final IdGroupingToken groupId, final ConjugateLink parent, final boolean directionAB) throws PlanItException {
    super(groupId, parent, directionAB);
  }

  /**
   * Copy constructor
   * 
   * @param conjugateLinkSegmentImpl to copy
   */
  protected ConjugateLinkSegmentImpl(ConjugateLinkSegmentImpl conjugateLinkSegmentImpl) {
    super(conjugateLinkSegmentImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentImpl clone() {
    return new ConjugateLinkSegmentImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Pair<LinkSegment, LinkSegment> getOriginalAdjcentEdgeSegments() {
    return (Pair<LinkSegment, LinkSegment>) ConjugateEdgeSegment.getOriginalAdjcentEdgeSegments(this);
  }

}
