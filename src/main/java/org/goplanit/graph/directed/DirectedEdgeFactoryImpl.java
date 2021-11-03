package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedEdgeFactory;
import org.goplanit.utils.graph.directed.DirectedEdges;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating directed edges on directed edges container
 * 
 * @author markr
 */
public class DirectedEdgeFactoryImpl extends GraphEntityFactoryImpl<DirectedEdge> implements DirectedEdgeFactory {

  /**
   * Constructor
   * 
   * @param groupId       to use
   * @param directedEdges to use
   */
  protected DirectedEdgeFactoryImpl(final IdGroupingToken groupId, final DirectedEdges directedEdges) {
    super(groupId, directedEdges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdge registerNew(DirectedVertex vertexA, DirectedVertex vertexB, boolean registerOnVertices) throws PlanItException {
    final DirectedEdge newEdge = new DirectedEdgeImpl(getIdGroupingToken(), vertexA, vertexB);
    getGraphEntities().register(newEdge);
    if (registerOnVertices) {
      vertexA.addEdge(newEdge);
      vertexB.addEdge(newEdge);
    }
    return newEdge;
  }

}
