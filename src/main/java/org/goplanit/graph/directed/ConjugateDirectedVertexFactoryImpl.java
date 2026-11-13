package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating vertices on vertices container
 * 
 * @author markr
 */
public class ConjugateDirectedVertexFactoryImpl
        extends GraphEntityFactoryImpl<ConjugateDirectedVertex> implements ConjugateDirectedVertexFactory {

  /**
   * Constructor
   * 
   * @param groupId            to use
   * @param conjugatedVertices to use
   */
  protected ConjugateDirectedVertexFactoryImpl(
          final IdGroupingToken groupId, final ConjugateDirectedVertices conjugatedVertices) {
    super(groupId, conjugatedVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertex createNew(final EdgeSegment original) {
    return new ConjugateDirectedVertexImpl(getIdGroupingToken(), original);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertex registerNew(final EdgeSegment original) {
    final ConjugateDirectedVertex newConjugateVertex = createNew(original);
    getGraphEntities().register(newConjugateVertex);
    return newConjugateVertex;
  }

}
