package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegmentFactory;
import org.goplanit.utils.network.layer.physical.LinkSegments;

/**
 * 
 * Link segments primary managed container implementation
 * 
 * @author markr
 *
 */
public class LinkSegmentsImpl extends ManagedIdEntitiesImpl<LinkSegment> implements LinkSegments, ManagedIdEntities<LinkSegment> {

  /** factory to use */
  private final LinkSegmentFactory linkSegmentFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public LinkSegmentsImpl(final IdGroupingToken groupId) {
    super(LinkSegment::getId, LinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.linkSegmentFactory = new LinkSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId            to use for creating ids for instances
   * @param linkSegmentFactory the factory to use
   */
  public LinkSegmentsImpl(final IdGroupingToken groupId, LinkSegmentFactory linkSegmentFactory) {
    super(LinkSegment::getId, LinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.linkSegmentFactory = linkSegmentFactory;
  }

  /**
   * Copy constructor
   * 
   * @param linkSegmentsImpl to copy
   */
  public LinkSegmentsImpl(LinkSegmentsImpl linkSegmentsImpl) {
    super(linkSegmentsImpl);
    this.linkSegmentFactory = linkSegmentsImpl.linkSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegmentFactory getFactory() {
    return linkSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional link segment id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), LinkSegment.LINK_SEGMENT_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegmentsImpl clone() {
    return new LinkSegmentsImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), LinkSegment.LINK_SEGMENT_ID_CLASS);
    super.reset();
  }

}
