package org.planit.network.layer.service;

import org.planit.graph.GraphEntitiesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegFactory;
import org.planit.utils.network.layer.service.ServiceLegs;

/**
 * Container class for managing service legs within a service network
 * 
 * @author markr
 *
 */
public class ServiceLegsImpl extends GraphEntitiesImpl<ServiceLeg> implements ServiceLegs {

  /** factory to use */
  private final ServiceLegFactory serviceLegFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ServiceLegsImpl(final IdGroupingToken groupId) {
    super(ServiceLeg::getId, ServiceLeg.EDGE_ID_CLASS);
    this.serviceLegFactory = new ServiceLegFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId           to use for creating ids for instances
   * @param serviceLegFactory the factory to use
   */
  public ServiceLegsImpl(final IdGroupingToken groupId, ServiceLegFactory serviceLegFactory) {
    super(ServiceLeg::getId, ServiceLeg.EDGE_ID_CLASS);
    this.serviceLegFactory = serviceLegFactory;
  }

  /**
   * Copy constructor
   * 
   * @param serviceNodesImpl to copy
   */
  public ServiceLegsImpl(ServiceLegsImpl serviceNodesImpl) {
    super(serviceNodesImpl);
    this.serviceLegFactory = serviceNodesImpl.serviceLegFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegFactory getFactory() {
    return serviceLegFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegsImpl clone() {
    return new ServiceLegsImpl(this);
  }

}