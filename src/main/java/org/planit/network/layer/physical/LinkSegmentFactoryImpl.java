package org.planit.network.layer.physical;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.LinkSegmentFactory;
import org.planit.utils.network.layer.physical.LinkSegments;

/**
 * Factory for creating link segments on link segments container
 * 
 * @author markr
 */
public class LinkSegmentFactoryImpl extends GraphEntityFactoryImpl<LinkSegment> implements LinkSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId      to use
   * @param linkSegments to use
   */
  protected LinkSegmentFactoryImpl(final IdGroupingToken groupId, LinkSegments linkSegments) {
    super(groupId, linkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegment create(final Link parentLink, final boolean directionAB) throws PlanItException {
    final LinkSegment edgeSegment = new LinkSegmentImpl(getIdGroupingToken(), directionAB);
    edgeSegment.setParent(parentLink);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegment registerNew(final Link parentLink, final boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    final LinkSegment edgeSegment = new LinkSegmentImpl(getIdGroupingToken(), parentLink, directionAb);
    getGraphEntities().register(edgeSegment);

    if (registerOnNodeAndLink) {
      parentLink.registerEdgeSegment(edgeSegment, directionAb);
      parentLink.getVertexA().addEdgeSegment(edgeSegment);
      parentLink.getVertexB().addEdgeSegment(edgeSegment);
    }
    return edgeSegment;
  }

}
