package org.goplanit.network.layer.service;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceLegSegmentFactory;
import org.goplanit.utils.network.layer.service.ServiceLegSegments;

/**
 * Implementation of ServiceLegSegments container.
 * 
 * @author markr
 *
 */
public class ServiceLegSegmentsImpl extends ManagedIdEntitiesImpl<ServiceLegSegment> implements ServiceLegSegments {

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
   * @param serviceNodesImpl to copy
   */
  public ServiceLegSegmentsImpl(ServiceLegSegmentsImpl serviceNodesImpl) {
    super(serviceNodesImpl);
    this.serviceLegSegmentFactory =
            new ServiceLegSegmentFactoryImpl(serviceNodesImpl.serviceLegSegmentFactory.getIdGroupingToken(), this);
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
  public ServiceLegSegmentsImpl clone() {
    return new ServiceLegSegmentsImpl(this);
  }

}
