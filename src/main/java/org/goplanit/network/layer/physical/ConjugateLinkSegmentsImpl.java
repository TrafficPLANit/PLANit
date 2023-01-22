package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegmentFactory;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegments;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * 
 * Link segments primary managed container implementation
 * 
 * @author markr
 *
 */
public class ConjugateLinkSegmentsImpl extends ManagedIdEntitiesImpl<ConjugateLinkSegment> implements ConjugateLinkSegments {

  /** factory to use */
  private final ConjugateLinkSegmentFactory factory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConjugateLinkSegmentsImpl(final IdGroupingToken groupId) {
    super(ConjugateLinkSegment::getId, LinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.factory = new ConjugateLinkSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   * @param factory the factory to use
   */
  public ConjugateLinkSegmentsImpl(final IdGroupingToken groupId, ConjugateLinkSegmentFactory factory) {
    super(ConjugateLinkSegment::getId, LinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param conjugateLinkSegmentsImpl to copy
   */
  public ConjugateLinkSegmentsImpl(ConjugateLinkSegmentsImpl conjugateLinkSegmentsImpl) {
    super(conjugateLinkSegmentsImpl);
    this.factory =
            new ConjugateLinkSegmentFactoryImpl(conjugateLinkSegmentsImpl.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentsImpl clone() {
    return new ConjugateLinkSegmentsImpl(this);
  }

}
