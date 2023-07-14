package org.goplanit.network.layer.service;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.network.layer.service.ServiceNodeFactory;
import org.goplanit.utils.network.layer.service.ServiceNodes;

/**
 * Factory for creating nodes on nodes container
 * 
 * @author markr
 */
public class ServiceNodeFactoryImpl extends GraphEntityFactoryImpl<ServiceNode> implements ServiceNodeFactory {

  /**
   * Constructor
   * 
   * @param groupId      to use
   * @param serviceNodes to use
   */
  protected ServiceNodeFactoryImpl(final IdGroupingToken groupId, final ServiceNodes serviceNodes) {
    super(groupId, serviceNodes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeImpl createNew() {
    return new ServiceNodeImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNode registerNew() {
    final ServiceNodeImpl newServiceNode = createNew();
    getGraphEntities().register(newServiceNode);
    return newServiceNode;
  }

}
