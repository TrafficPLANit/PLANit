package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdgeFactory;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdges;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating conjugate directed edges on conjugate directed edges container
 * 
 * @author markr
 */
public class ConjugateDirectedEdgeFactoryImpl extends GraphEntityFactoryImpl<ConjugateDirectedEdge> implements ConjugateDirectedEdgeFactory {

  /**
   * Constructor
   * 
   * @param groupId                to use
   * @param conjugateDirectedEdges to use
   */
  protected ConjugateDirectedEdgeFactoryImpl(final IdGroupingToken groupId, final ConjugateDirectedEdges conjugateDirectedEdges) {
    super(groupId, conjugateDirectedEdges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdge registerNew(ConjugateDirectedVertex vertexA, ConjugateDirectedVertex vertexB, DirectedEdge originalEdge1, DirectedEdge originalEdge2,
      boolean registerOnVertices) throws PlanItException {
    final var newConjugateEdge = new ConjugateDirectedEdgeImpl(getIdGroupingToken(), vertexA, vertexB, originalEdge1, originalEdge2);
    getGraphEntities().register(newConjugateEdge);
    if (registerOnVertices) {
      vertexA.addEdge(newConjugateEdge);
      vertexB.addEdge(newConjugateEdge);
    }
    return newConjugateEdge;
  }

}
