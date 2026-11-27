package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating conjugate directed edges on conjugate directed edges container
 * 
 * @author markr
 */
public class ConjugateDirectedEdgeFactoryImpl extends GraphEntityFactoryImpl<ConjugateDirectedEdge>
        implements ConjugateDirectedEdgeFactory {

  /**
   * Constructor
   * 
   * @param groupId                to use
   * @param conjugateDirectedEdges to use
   */
  protected ConjugateDirectedEdgeFactoryImpl(
          final IdGroupingToken groupId, final ConjugateDirectedEdges conjugateDirectedEdges) {
    super(groupId, conjugateDirectedEdges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdge registerNew(
          ConjugateDirectedVertex vertexA,
          ConjugateDirectedVertex vertexB,
          boolean registerOnVertices,
          EdgeSegment original1,
          EdgeSegment original2){
    final var newConjugateEdge = new ConjugateDirectedEdgeImpl<>(
            getIdGroupingToken(), vertexA, vertexB, original1, original2);
    getGraphEntities().register(newConjugateEdge);
    if (registerOnVertices) {
      vertexA.addEdge(newConjugateEdge);
      vertexB.addEdge(newConjugateEdge);
    }
    return newConjugateEdge;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdge registerNew(
          ConjugateDirectedVertex vertexA,
          ConjugateDirectedVertex vertexB,
          boolean registerOnVertices,
          EdgeSegment original1,
          EdgeSegment original2,
          boolean deriveXmlIdFromOriginalEdges,
          String xmlIdPostFix){
    final var newConjugateEdge = registerNew(vertexA, vertexB, registerOnVertices, original1, original2);
    newConjugateEdge.populateXmlId(deriveXmlIdFromOriginalEdges, xmlIdPostFix);
    return newConjugateEdge;
  }

}
