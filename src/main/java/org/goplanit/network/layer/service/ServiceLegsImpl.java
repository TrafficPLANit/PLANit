package org.goplanit.network.layer.service;

import org.goplanit.network.layer.physical.ConjugateNodesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegFactory;
import org.goplanit.utils.network.layer.service.ServiceLegs;

import java.util.function.BiConsumer;

/**
 * Container class for managing service legs within a service network
 * 
 * @author markr
 *
 */
public class ServiceLegsImpl extends ManagedIdEntitiesImpl<ServiceLeg> implements ServiceLegs {

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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ServiceLegsImpl(ServiceLegsImpl other, boolean deepCopy, BiConsumer<ServiceLeg,ServiceLeg> mapper) {
    super(other, deepCopy, mapper);
    this.serviceLegFactory =
            new ServiceLegFactoryImpl(other.serviceLegFactory.getIdGroupingToken(), this);
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
  public ServiceLegsImpl shallowClone() {
    return new ServiceLegsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegsImpl deepClone() {
    return new ServiceLegsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegsImpl deepCloneWithMapping(BiConsumer<ServiceLeg,ServiceLeg> mapper) {
    return new ServiceLegsImpl(this, true, mapper);
  }

}
