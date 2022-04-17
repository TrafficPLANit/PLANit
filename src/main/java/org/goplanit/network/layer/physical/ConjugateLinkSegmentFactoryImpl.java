package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegmentFactory;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegments;

/**
 * Factory for creating conjugate link segments on container
 * 
 * @author markr
 */
public class ConjugateLinkSegmentFactoryImpl extends GraphEntityFactoryImpl<ConjugateLinkSegment> implements ConjugateLinkSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId   to use
   * @param container to use
   */
  protected ConjugateLinkSegmentFactoryImpl(final IdGroupingToken groupId, ConjugateLinkSegments container) {
    super(groupId, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegment create(final ConjugateLink parent, final boolean directionAB) throws PlanItException {
    final ConjugateLinkSegment edgeSegment = new ConjugateLinkSegmentImpl(getIdGroupingToken(), parent, directionAB);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegment registerNew(final ConjugateLink parent, final boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    final ConjugateLinkSegment edgeSegment = create(parent, directionAb);
    getGraphEntities().register(edgeSegment);

    if (registerOnNodeAndLink) {
      parent.registerEdgeSegment(edgeSegment, directionAb);
    }
    return edgeSegment;
  }

}
