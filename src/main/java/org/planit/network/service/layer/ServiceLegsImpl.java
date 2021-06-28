package org.planit.network.service.layer;

import org.planit.graph.EdgesWrapper;
import org.planit.utils.graph.Edges;
import org.planit.utils.network.service.ServiceLeg;
import org.planit.utils.network.service.ServiceLegs;

/**
 * Container class for managing service legs within a service network
 * 
 * @author markr
 *
 */
public class ServiceLegsImpl<SL extends ServiceLeg> extends EdgesWrapper<SL> implements ServiceLegs<SL> {

  /**
   * Constructor
   * 
   * @param edges containing the service legs
   */
  public ServiceLegsImpl(Edges<SL> edges) {
    super(edges);
  }

}
