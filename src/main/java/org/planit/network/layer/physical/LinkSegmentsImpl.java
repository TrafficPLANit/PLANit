package org.planit.network.layer.physical;

import org.planit.graph.GraphEntitiesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.LinkSegmentFactory;
import org.planit.utils.network.layer.physical.LinkSegments;

/**
 * 
 * Link segments container implementation
 * 
 * @author markr
 *
 */
public class LinkSegmentsImpl extends GraphEntitiesImpl<LinkSegment> implements LinkSegments {

  /** factory to use */
  private final LinkSegmentFactory linkSegmentFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public LinkSegmentsImpl(final IdGroupingToken groupId) {
    super(LinkSegment::getId);
    this.linkSegmentFactory = new LinkSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId            to use for creating ids for instances
   * @param linkSegmentFactory the factory to use
   */
  public LinkSegmentsImpl(final IdGroupingToken groupId, LinkSegmentFactory linkSegmentFactory) {
    super(LinkSegment::getId);
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
  public LinkSegmentsImpl clone() {
    return new LinkSegmentsImpl(this);
  }

}
