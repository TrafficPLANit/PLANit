package org.planit.network.layer.service;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.service.ServiceNode;
import org.planit.utils.network.layer.service.ServiceNodeFactory;
import org.planit.utils.network.layer.service.ServiceNodes;

/**
 * Factory for creating nodes on nodes container
 * 
 * @author markr
 */
public class ServiceNodeFactoryImpl extends GraphEntityFactoryImpl<ServiceNode> implements ServiceNodeFactory {

  /**
   * Constructor
   * 
   * @param groupId  to use
   * @param serviceNodes to use
   */
  protected ServiceNodeFactoryImpl(final IdGroupingToken groupId, final ServiceNodes serviceNodes) {
    super(groupId, serviceNodes);
  }

  
  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNodeImpl createNew(Node networkNode) {
    return new ServiceNodeImpl(getIdGroupingToken(), networkNode);
  }  

  /**
   * {@inheritDoc}
   */  
  @Override
  public ServiceNode registerNew(Node networkNode) {
    final ServiceNodeImpl newServiceNode = createNew(networkNode);
    getGraphEntities().register(newServiceNode);
    return newServiceNode;
  }

}
