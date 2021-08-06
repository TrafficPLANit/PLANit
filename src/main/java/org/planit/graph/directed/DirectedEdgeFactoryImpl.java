package org.planit.graph.directed;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedEdgeFactory;
import org.planit.utils.graph.directed.DirectedEdges;
import org.planit.utils.graph.directed.DirectedVertex;
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