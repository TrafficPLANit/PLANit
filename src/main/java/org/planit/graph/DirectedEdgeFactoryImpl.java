package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedEdgeFactory;
import org.planit.utils.graph.DirectedEdges;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;

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
