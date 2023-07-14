package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertexFactory;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertices;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating vertices on vertices container
 * 
 * @author markr
 */
public class ConjugateDirectedVertexFactoryImpl extends GraphEntityFactoryImpl<ConjugateDirectedVertex> implements ConjugateDirectedVertexFactory {

  /**
   * Constructor
   * 
   * @param groupId            to use
   * @param conjugatedVertices to use
   */
  protected ConjugateDirectedVertexFactoryImpl(final IdGroupingToken groupId, final ConjugateDirectedVertices conjugatedVertices) {
    super(groupId, conjugatedVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertex createNew(final DirectedEdge originalEdge) {
    return new ConjugateDirectedVertexImpl(getIdGroupingToken(), originalEdge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertex registerNew(final DirectedEdge originalEdge) {
    final ConjugateDirectedVertex newConjugateVertex = createNew(originalEdge);
    getGraphEntities().register(newConjugateVertex);
    return newConjugateVertex;
  }

}
