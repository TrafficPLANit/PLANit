package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class GraphBuilderImpl implements GraphBuilder<Vertex, Edge> {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(GraphBuilderImpl.class.getCanonicalName());

  /** the id group token */
  protected IdGroupingToken groupToken;

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex createVertex() {
    return new DirectedVertexImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge createEdge(Vertex vertexA, Vertex vertexB, final double length) throws PlanItException {
    return new EdgeImpl(getIdGroupingToken(), vertexA, vertexB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupToken) {
    this.groupToken = groupToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return this.groupToken;
  }

}
