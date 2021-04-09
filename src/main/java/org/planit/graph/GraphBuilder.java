package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGroupingToken;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements
 * 
 * @author markr
 *
 */
public interface GraphBuilder<V extends Vertex, E extends Edge> {

  /**
   * Create a new vertex instance
   * 
   * @return created vertex
   */
  public V createVertex();

  /**
   * Create a new link instance
   * 
   * @param vertexA the first vertex in this edge
   * @param vertexB the second vertex in this edge
   * @param length  the length (in km)
   * @return created edge
   * @throws PlanItException thrown if there is an error
   */
  public E createEdge(final Vertex vertexA, final Vertex vertexB, final double length) throws PlanItException;

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

  /**
   * recreate the ids for all passed in edges
   * 
   * @param edges to recreate ids for
   */
  public void recreateIds(Edges<? extends E> edges);

  /**
   * recreate the ids for all passed in vertices
   * 
   * @param vertices to recreate ids for
   */
  public void recreateIds(Vertices<? extends V> vertices);

  /**
   * create a shallow copy of the passed in edge, albeit with unique internal ids
   * 
   * @param edgeToCopy the edge to copy
   * @return new edge based on passed in edge
   */
  public E createUniqueCopyOf(E edgeToCopy);

}
