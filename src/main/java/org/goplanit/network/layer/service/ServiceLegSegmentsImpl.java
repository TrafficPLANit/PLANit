package org.goplanit.network.layer.service;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceLegSegmentFactory;
import org.goplanit.utils.network.layer.service.ServiceLegSegments;

import java.util.function.BiConsumer;

/**
 * Implementation of ServiceLegSegments container.
 * 
 * @author markr
 *
 */
public class ServiceLegSegmentsImpl extends ManagedGraphEntitiesImpl<ServiceLegSegment> implements ServiceLegSegments {

  /** factory to use */
  private final ServiceLegSegmentFactory serviceLegSegmentFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ServiceLegSegmentsImpl(final IdGroupingToken groupId) {
    super(ServiceLegSegment::getId, ServiceLegSegment.EDGE_SEGMENT_ID_CLASS);
    this.serviceLegSegmentFactory = new ServiceLegSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                  to use for creating ids for instances
   * @param serviceLegSegmentFactory the factory to use
   */
  public ServiceLegSegmentsImpl(final IdGroupingToken groupId, ServiceLegSegmentFactory serviceLegSegmentFactory) {
    super(ServiceLegSegment::getId, ServiceLegSegment.EDGE_SEGMENT_ID_CLASS);
    this.serviceLegSegmentFactory = serviceLegSegmentFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ServiceLegSegmentsImpl(ServiceLegSegmentsImpl other, boolean deepCopy, BiConsumer<ServiceLegSegment,ServiceLegSegment> mapper) {
    super(other,deepCopy, mapper);
    this.serviceLegSegmentFactory =
            new ServiceLegSegmentFactoryImpl(other.serviceLegSegmentFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegmentFactory getFactory() {
    return serviceLegSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegmentsImpl shallowClone() {
    return new ServiceLegSegmentsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegmentsImpl deepClone() {
    return new ServiceLegSegmentsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegmentsImpl deepCloneWithMapping(BiConsumer<ServiceLegSegment,ServiceLegSegment> mapper) {
    return new ServiceLegSegmentsImpl(this, true, mapper);
  }

}
