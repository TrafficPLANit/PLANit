package org.goplanit.network.layer.service;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;

/**
 * A service leg segment implementation.
 * 
 * @author markr
 *
 */
public class ServiceLegSegmentImpl extends EdgeSegmentImpl<ServiceLeg> implements ServiceLegSegment {

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
   */
  protected ServiceLegSegmentImpl(final IdGroupingToken tokenId, final ServiceLeg parentLeg, final boolean directionAB) {
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
    return (ServiceLeg) getParent();
  }
}
