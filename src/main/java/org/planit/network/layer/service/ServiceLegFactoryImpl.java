package org.planit.network.layer.service;

import java.util.List;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegFactory;
import org.planit.utils.network.layer.service.ServiceNode;

/**
 * Factory for creating service legs on service leg container
 * 
 * @author markr
 */
public class ServiceLegFactoryImpl extends GraphEntityFactoryImpl<ServiceLeg> implements ServiceLegFactory {

  protected void registerOnNodes(ServiceLegImpl leg) {
    leg.getVertexA().addEdge(leg);
    leg.getVertexB().addEdge(leg);
  }
  
  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   * @param serviceLegs        to register the created instances on
   */
  public ServiceLegFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<ServiceLeg> serviceLegs) {
    super(groupIdToken, serviceLegs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegImpl registerNew(final ServiceNode nodeA, final ServiceNode nodeB, boolean registerOnNodes) {
    ServiceLegImpl newServiceLeg = new ServiceLegImpl(getIdGroupingToken(), nodeA, nodeB);
    if (registerOnNodes) {
      registerOnNodes(newServiceLeg);
    }
    return newServiceLeg;
  }


  /**
   * {@inheritDoc}
   */  
  @Override
  public ServiceLeg registerNew(final ServiceNode nodeA, final ServiceNode nodeB, final List<Link> networkLayerLinks, boolean registerOnNodes) {
    ServiceLegImpl newServiceLeg = new ServiceLegImpl(getIdGroupingToken(), nodeA, nodeB, networkLayerLinks);
    if (registerOnNodes) {
      registerOnNodes(newServiceLeg);
    }
    return newServiceLeg;
  }

}
