package org.planit.network.layer.physical;

import org.planit.graph.EdgeSegmentsWrapper;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.LinkSegments;

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

}
