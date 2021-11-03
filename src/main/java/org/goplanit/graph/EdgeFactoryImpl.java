package org.goplanit.graph;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeFactory;
import org.goplanit.utils.graph.Edges;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating edges on edges container
 * 
 * @author markr
 */
public class EdgeFactoryImpl extends GraphEntityFactoryImpl<Edge> implements EdgeFactory {

  /**
   * Constructor
   * 
   * @param groupId to use
   * @param edges   to use
   */
  protected EdgeFactoryImpl(final IdGroupingToken groupId, final Edges edges) {
    super(groupId, edges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge registerNew(Vertex vertexA, Vertex vertexB, boolean registerOnVertices) throws PlanItException {
    final Edge newEdge = new EdgeImpl(getIdGroupingToken(), vertexA, vertexB);
    getGraphEntities().register(newEdge);
    if (registerOnVertices) {
      vertexA.addEdge(newEdge);
      vertexB.addEdge(newEdge);
    }
    return newEdge;
  }

}
