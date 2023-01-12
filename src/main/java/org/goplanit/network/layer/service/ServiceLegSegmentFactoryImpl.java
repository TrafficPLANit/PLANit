package org.goplanit.network.layer.service;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceLegSegmentFactory;

import java.util.List;

/**
 * Factory for creating service leg segments on service leg segment container
 * 
 * @author markr
 */
public class ServiceLegSegmentFactoryImpl extends GraphEntityFactoryImpl<ServiceLegSegment> implements ServiceLegSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupIdToken       to use for creating element ids
   * @param serviceLegSegments to register the created instances on
   */
  public ServiceLegSegmentFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<ServiceLegSegment> serviceLegSegments) {
    super(groupIdToken, serviceLegSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegmentImpl registerNew(final ServiceLeg parentLeg, final boolean directionAb, boolean registerOnServiceNodeAndLeg) {
    return registerNew(parentLeg, directionAb, null, registerOnServiceNodeAndLeg);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegmentImpl registerNew(ServiceLeg parentLeg, boolean directionAb, final List<Link> networkLayerLinks, boolean registerOnServiceNodeAndLeg){
    final ServiceLegSegmentImpl legSegment = new ServiceLegSegmentImpl(getIdGroupingToken(), parentLeg, directionAb, networkLayerLinks);
    getGraphEntities().register(legSegment);

    if (registerOnServiceNodeAndLeg) {
      parentLeg.registerEdgeSegment(legSegment, directionAb);
    }
    return legSegment;
  }

}
