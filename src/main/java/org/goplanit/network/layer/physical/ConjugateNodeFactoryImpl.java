package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.*;

/**
 * Factory for creating nodes on conjugate nodes container.
 * 
 * @author markr
 */
public class ConjugateNodeFactoryImpl extends GraphEntityFactoryImpl<ConjugateNode> implements ConjugateNodeFactory {

  /**
   * Constructor
   * 
   * @param groupId   to use
   * @param container to use
   */
  protected ConjugateNodeFactoryImpl(final IdGroupingToken groupId, final ConjugateNodes container) {
    super(groupId, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNode createNew(final LinkSegment originalLinkSegment) {
    return new ConjugateNodeImpl(getIdGroupingToken(), originalLinkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNode registerNew(
          final LinkSegment originalLinkSegment, boolean deriveFromOriginalEdge, String xmlIdPostFix) {
    final ConjugateNode newEntity = createNew(originalLinkSegment);
    newEntity.populateXmlId(deriveFromOriginalEdge, xmlIdPostFix);
    getGraphEntities().register(newEntity);
    return newEntity;
  }

}
