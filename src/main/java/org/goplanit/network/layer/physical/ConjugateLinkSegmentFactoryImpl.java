package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntityFactoryImpl;
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

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateLinkSegmentFactoryImpl.class.getCanonicalName());

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
  public ConjugateLinkSegment create(final ConjugateLink parent, final boolean directionAb) {
    var originalEdgeSegments = parent.getOriginalAdjacentEdgeSegments(directionAb);
    /* only proceed when it is possible to create this conjugate */
    if (originalEdgeSegments.anyIsNull()) {
      LOGGER.warning(String.format("Unable to create conjugate link segment on conjugate link %s (directionAb: %s)", parent.getXmlId(), Boolean.toString(directionAb)));
      return null;
    }

    final ConjugateLinkSegment edgeSegment = new ConjugateLinkSegmentImpl(getIdGroupingToken(), parent, directionAb);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegment registerNew(final ConjugateLink parent, final boolean directionAb, boolean registerOnNodeAndLink) {
    final ConjugateLinkSegment edgeSegment = create(parent, directionAb);
    getGraphEntities().register(edgeSegment);

    if (registerOnNodeAndLink) {
      parent.registerEdgeSegment(edgeSegment, directionAb);
    }
    return edgeSegment;
  }

}
