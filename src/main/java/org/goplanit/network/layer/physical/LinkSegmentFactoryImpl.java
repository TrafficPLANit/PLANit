package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegmentFactory;
import org.goplanit.utils.network.layer.physical.LinkSegments;

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
    final LinkSegment edgeSegment = new LinkSegmentImpl<Link>(getIdGroupingToken(), parentLink, directionAB);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegment registerNew(final Link parentLink, final boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    final LinkSegment edgeSegment = create(parentLink, directionAb);
    getGraphEntities().register(edgeSegment);

    if (registerOnNodeAndLink) {
      parentLink.registerEdgeSegment(edgeSegment, directionAb);
    }
    return edgeSegment;
  }

}
