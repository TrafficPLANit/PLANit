package org.goplanit.graph.directed.acyclic;

import java.util.Collection;
import java.util.Set;

import org.goplanit.utils.graph.directed.DirectedSubGraph;
import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * 
 * An acyclic sub graph contains a subset of the full graph without cycles. The active subset of the graph is tracked by explicitly registering edge segments. Edge segments are by
 * definition directed.
 * <p>
 * A topological sort on the current state of the graph allows for fast traversal of the graph for various algorithms (shortest path). It also reveals if the graph is still
 * acyclic.
 * <p>
 * To allow for maximum flexibility we do not require any information on how the edges, vertices, edge segments of this graph are configured, i.e., they may be an amalgamation of
 * other (combined) graphs. As long as their internal structure (downstream, upstream vertices, exit and entry segments) represent a valid acyclic graph structure, the any
 * implementation should be able to deal with it.
 * 
 * 
 * @author markr
 *
 */
public interface ACyclicSubGraph extends DirectedSubGraph, Iterable<DirectedVertex> {

  /**
   * Collect the root vertices of this acyclic subgraph
   * 
   * @return root vertices
   */
  public abstract Set<DirectedVertex> getRootVertices();

  /**
   * Verify if vertex is registered as root vertex on this dag
   * 
   * @param rootVertex to verify
   * @return true if present, false otherwise
   */
  public default boolean containsRootVertex(DirectedVertex rootVertex) {
    return getRootVertices().contains(rootVertex);
  }

  /**
   * Add a root vertex to the subgraph
   * 
   * @param rootVertex to add
   */
  public abstract void addRootVertex(DirectedVertex rootVertex);

  /**
   * Perform a topological sort on this graph. It is expected that this is conducted before any operations that require this sorting to be in place are invoked, e.g., min-max path
   * tree for example.
   * 
   * @param update when true the topological sort is conducted based on the current state of the subgraph, when false the most recent (if any) result is returned
   * @return return topological sorting found, null when it was found not to be possible to create a topological sorting
   */
  public abstract Collection<DirectedVertex> topologicalSort(boolean update);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ACyclicSubGraph clone();

}
