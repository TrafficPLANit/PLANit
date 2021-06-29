package org.planit.network.layer.service;

import org.planit.graph.EdgeSegmentsWrapper;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.network.layer.service.ServiceLegSegments;

/**
 * Implementation of ServiceLegSegments container.
 * 
 * @author markr
 *
 * @param <SLS> type of service leg segments
 */
public class ServiceLegSegmentsImpl<SLS extends ServiceLegSegment> extends EdgeSegmentsWrapper<SLS> implements ServiceLegSegments<SLS> {

  /**
   * Constructor
   * 
   * @param edgeSegments to wrap
   */
  public ServiceLegSegmentsImpl(EdgeSegments<SLS> edgeSegments) {
    super(edgeSegments);
  }

}
