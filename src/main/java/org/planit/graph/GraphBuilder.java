package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements
 * 
 * @author markr
 *
 */
public interface GraphBuilder {

  /**
   * Create a new vertex instance
   * 
   * @return created vertex
   */
  public Vertex createVertex();

  /**
   * Create a new link instance
   * 
   * @param vertexA the first vertex in this edge
   * @param vertexB the second vertex in this edge
   * @param lenght  the length (in km)
   * @return created edge
   * @throws PlanItException thrown if there is an error
   */
  public Edge createEdge(final Vertex vertexA, final Vertex vertexB, final double length) throws PlanItException;

  /**
   * Create a new physical link segment instance
   * 
   * @param parentEdge  the parent edge of the edge segment
   * @param directionAB direction of travel
   * @return edgeSegment the created edge segment
   * @throws PlanItException thrown if error
   */
  public EdgeSegment createEdgeSegment(Edge parentEdge, boolean directionAB) throws PlanItException;

  /**
   * Each builder needs a group if token to allow all underlying factory methods to generated ids uniquely tied to the group the entities belong to
   * 
   * @param groupId, contiguous id generation within this group for instances created with the factory methods
   */
  public void setIdGroupingToken(IdGroupingToken groupId);

  /**
   * Collect the id grouping token used by this builder
   * 
   * @return idGroupingToken the id grouping token used by this builder
   */
  public IdGroupingToken getIdGroupingToken();

}
