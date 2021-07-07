package org.planit.network.layer.service;

import org.planit.graph.GraphEntitiesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.service.ServiceNode;
import org.planit.utils.network.layer.service.ServiceNodeFactory;
import org.planit.utils.network.layer.service.ServiceNodes;

/**
 * 
 * ServiceNodes implementation.
 * 
 * @author markr
 *
 */
public class ServiceNodesImpl extends GraphEntitiesImpl<ServiceNode> implements ServiceNodes {


  /** factory to use */
  private final ServiceNodeFactory serviceNodeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ServiceNodesImpl(final IdGroupingToken groupId) {
    super(ServiceNode::getId);
    this.serviceNodeFactory = new ServiceNodeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param serviceNodeFactory the factory to use
   */
  public ServiceNodesImpl(final IdGroupingToken groupId, ServiceNodeFactory serviceNodeFactory) {
    super(ServiceNode::getId);
    this.serviceNodeFactory = serviceNodeFactory;
  }

  /**
   * Copy constructor
   * 
   * @param serviceNodesImpl to copy
   */
  public ServiceNodesImpl(ServiceNodesImpl serviceNodesImpl) {
    super(serviceNodesImpl);
    this.serviceNodeFactory = serviceNodesImpl.serviceNodeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeFactory getFactory() {
    return serviceNodeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodesImpl clone() {
    return new ServiceNodesImpl(this);
  }

}
