package org.planit.network.layer.macroscopic;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentFactory;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

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
   * Copy constructor
   * 
   * @param linkSegmentsImpl to copy
   */
  public MacroscopicLinkSegmentsImpl(MacroscopicLinkSegmentsImpl linkSegmentsImpl) {
    super(linkSegmentsImpl);
    this.linkSegmentFactory = linkSegmentsImpl.linkSegmentFactory;
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
  public MacroscopicLinkSegmentsImpl clone() {
    return new MacroscopicLinkSegmentsImpl(this);
  }

}
