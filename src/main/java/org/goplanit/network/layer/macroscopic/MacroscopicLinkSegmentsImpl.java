package org.goplanit.network.layer.macroscopic;

import org.goplanit.mode.ModesImpl;
import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentFactory;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.function.BiConsumer;

/**
 * 
 * Link segments container implementation
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentsImpl extends ManagedGraphEntitiesImpl<MacroscopicLinkSegment> implements MacroscopicLinkSegments {

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
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public MacroscopicLinkSegmentsImpl(MacroscopicLinkSegmentsImpl other, boolean deepCopy, BiConsumer<MacroscopicLinkSegment,MacroscopicLinkSegment> mapper) {
    super(other, deepCopy, mapper);
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
   *
   * todo: this method should ideally exist in a linkSegments container class rather than this more specific one
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
  public MacroscopicLinkSegmentsImpl shallowClone() {
    return new MacroscopicLinkSegmentsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentsImpl deepClone() {
    return new MacroscopicLinkSegmentsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentsImpl deepCloneWithMapping(BiConsumer<MacroscopicLinkSegment,MacroscopicLinkSegment> mapper) {
    return new MacroscopicLinkSegmentsImpl(this, true, mapper);
  }

}
