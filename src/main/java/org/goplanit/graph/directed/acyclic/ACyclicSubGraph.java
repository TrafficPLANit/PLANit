package org.goplanit.graph.directed.acyclic;

import java.util.Deque;
import java.util.Iterator;

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
   * Root vertex of this acyclic graph. root can either be a starting point or end point depending on the direction of the dag
   * 
   * @return root vertex
   */
  public abstract DirectedVertex getRootVertex();

  /**
   * Indicates if the direction of the graph is inverted, i.e., when inverted the root vertex is the final vertex and all other vertices precede it, otherwise it is a starting
   * point and all other vertices succeed it
   * 
   * @return true when inverted, false otherwise
   */
  public abstract boolean isDirectionInverted();

  /**
   * Perform a topological sort on this graph. It is expected that this is conducted before any operations that require this sorting to be in place are invoked, e.g., min-max path
   * tree for example.
   * 
   * @param update when true the topological sort is conducted based on the current state of the subgraph, when false the most recent (if any) result is returned
   * @return return topological sorting found, null when it was found not to be possible to create a topological sorting
   */
  public abstract Deque<DirectedVertex> topologicalSort(boolean update);

  /**
   * Collect iterator over topologically sorted vertices
   * 
   * @param update when true the topological sort is conducted based on the current state of the subgraph, when false the most recent (if any) result is returned
   * @return iterator
   */
  public default Iterator<DirectedVertex> getTopologicalIterator(boolean update) {
    return getTopologicalIterator(update, false);
  }

  /**
   * Collect iterator over topologically sorted vertices
   * 
   * @param update             when true the topological sort is conducted based on the current state of the subgraph, when false the most recent (if any) result is returned
   * @param descendingIterator when true, iterator direction is reversed, when false it is not
   * @return iterator
   */
  public default Iterator<DirectedVertex> getTopologicalIterator(boolean update, boolean descendingIterator) {
    var topologicallySortedVertices = topologicalSort(update);
    return descendingIterator ? topologicallySortedVertices.descendingIterator() : topologicallySortedVertices.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ACyclicSubGraph clone();

}
