package org.goplanit.network.layer.macroscopic;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * 
 * Link segments container implementation
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentsImpl extends ManagedIdEntitiesImpl<MacroscopicLinkSegment> implements MacroscopicLinkSegments {

  /** factory to use */
  private final MacroscopicLinkSegmentFactory linkSegmentFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public MacroscopicLinkSegmentsImpl(final IdGroupingToken groupId) {
    super(MacroscopicLinkSegment::getId, MacroscopicLinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.linkSegmentFactory = new MacroscopicLinkSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId            to use for creating ids for instances
   * @param linkSegmentFactory the factory to use
   */
  public MacroscopicLinkSegmentsImpl(final IdGroupingToken groupId, MacroscopicLinkSegmentFactory linkSegmentFactory) {
    super(MacroscopicLinkSegment::getId, MacroscopicLinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.linkSegmentFactory = linkSegmentFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  public MacroscopicLinkSegmentsImpl(MacroscopicLinkSegmentsImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.linkSegmentFactory =
            new MacroscopicLinkSegmentFactoryImpl(other.linkSegmentFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentFactory getFactory() {
    return linkSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentsImpl shallowClone() {
    return new MacroscopicLinkSegmentsImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentsImpl deepClone() {
    return new MacroscopicLinkSegmentsImpl(this, true);
  }

}
