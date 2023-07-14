package org.goplanit.network.layer.service;

import org.goplanit.network.layer.physical.ConjugateNodesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.network.layer.service.ServiceNodeFactory;
import org.goplanit.utils.network.layer.service.ServiceNodes;

import java.util.function.BiConsumer;

/**
 * 
 * ServiceNodes implementation.
 * 
 * @author markr
 *
 */
public class ServiceNodesImpl extends ManagedIdEntitiesImpl<ServiceNode> implements ServiceNodes {

  /** factory to use */
  private final ServiceNodeFactory serviceNodeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ServiceNodesImpl(final IdGroupingToken groupId) {
    super(ServiceNode::getId, ServiceNode.VERTEX_ID_CLASS);
    this.serviceNodeFactory = new ServiceNodeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId            to use for creating ids for instances
   * @param serviceNodeFactory the factory to use
   */
  public ServiceNodesImpl(final IdGroupingToken groupId, ServiceNodeFactory serviceNodeFactory) {
    super(ServiceNode::getId, ServiceNode.VERTEX_ID_CLASS);
    this.serviceNodeFactory = serviceNodeFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ServiceNodesImpl(ServiceNodesImpl other, boolean deepCopy, BiConsumer<ServiceNode,ServiceNode> mapper) {
    super(other, deepCopy, mapper);
    this.serviceNodeFactory =
            new ServiceNodeFactoryImpl(other.serviceNodeFactory.getIdGroupingToken(), this);
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
  public ServiceNodesImpl shallowClone() {
    return new ServiceNodesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodesImpl deepClone() {
    return new ServiceNodesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodesImpl deepCloneWithMapping(BiConsumer<ServiceNode,ServiceNode> mapper) {
    return new ServiceNodesImpl(this, true, mapper);
  }

}
