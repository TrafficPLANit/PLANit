package org.planit.network.service.layer;

import org.planit.graph.EdgeSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.service.ServiceLeg;
import org.planit.utils.network.service.ServiceLegSegment;

/**
 * A service leg segment implementation.
 * 
 * @author markr
 *
 */
public class ServiceLegSegmentImpl extends EdgeSegmentImpl implements ServiceLegSegment {

  /**
   * generated UID
   */
  private static final long serialVersionUID = 407229732625691579L;

  /**
   * Constructor
   *
   * @param tokenId     contiguous id generation within this group for instances of this class
   * @param parentLeg   parent leg of segment
   * @param directionAB direction of travel
   * @throws PlanItException thrown if error
   */
  protected ServiceLegSegmentImpl(final IdGroupingToken tokenId, final ServiceLeg parentLeg, final boolean directionAB) throws PlanItException {
    super(tokenId, parentLeg, directionAB);
  }

  /**
   * Copy constructor
   * 
   * @param serviceLegSegment to copy
   */
  protected ServiceLegSegmentImpl(ServiceLegSegmentImpl serviceLegSegment) {
    super(serviceLegSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLeg getParentLeg() {
    return (ServiceLeg) getParentEdge();
  }
}
