package org.planit.network.physical;

import org.planit.graph.EdgeSegmentsWrapper;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;

/**
 * 
 * Links implementation wrapper that simply utilises passed in edges of the desired generic type to delegate registration and creation of its links on
 * 
 * @author markr
 *
 * @param <LS> link segments type
 */
public class LinkSegmentsImpl<LS extends LinkSegment> extends EdgeSegmentsWrapper<LS> implements LinkSegments<LS> {

  /**
   * Constructor
   * 
   * @param edgeSegments to use to create and register link segments on
   */
  public LinkSegmentsImpl(final EdgeSegments<LS> edgeSegments) {
    super(edgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LS getByExternalId(String externalId) {
    for (LS linkSegment : getEdgeSegments()) {
      if (externalId.equals(linkSegment.getExternalId())) {
        return linkSegment;
      }
    }
    return null;
  }

}
