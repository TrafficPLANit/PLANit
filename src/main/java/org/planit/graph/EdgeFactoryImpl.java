package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeFactory;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;

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
