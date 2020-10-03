package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Graph;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class DirectedGraphBuilderImpl implements GraphBuilder<Vertex, Edge> {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected IdGroupingToken groupId;

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex createVertex() {
    return new VertexImpl(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge createEdge(Vertex vertexA, Vertex vertexB, final double length) throws PlanItException {
    return new EdgeImpl(groupId, vertexA, vertexB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupId) {
    this.groupId = groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return this.groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeIdGaps(Graph<Vertex, Edge> graph) {
    // TODO Auto-generated method stub
  }

}
