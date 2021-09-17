package org.planit.graph.directed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedEdges;
import org.planit.utils.graph.directed.DirectedGraph;
import org.planit.utils.graph.directed.DirectedSubGraph;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.directed.DirectedVertices;
import org.planit.utils.graph.directed.EdgeSegments;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.Pair;

/**
 * 
 * An acyclic sub graph contains a subset of the full graph without cycles. The active subset of the graph is tracked by explicitly registering edge segments. Edge segments are by
 * definition directed.
 * 
 * Whenever edge segments are added it is verified that no cycles are created. Also each edge segment that is added must connect to the existing subgraph's contents
 * 
 * <p>
 * <b>INCOMPLETE IMPLEMENTATION</b>
 * 
 * @author markr
 *
 */
public class ACyclicSubGraph<V extends DirectedVertices, E extends DirectedEdges, ES extends EdgeSegments> implements DirectedSubGraph<V, E, ES> {

  /**
   * The id of this acyclic sub graph
   */
  private final long id;

  /**
   * The parent graph where this is an acyclic subgraph of
   */
  DirectedGraph<V, E, ES> parentGraph;

  /**
   * root of the sub graph
   */
  DirectedVertex root;

  /**
   * list to store vertices (and its activated edge segments) in topological order We only store outgoing active edge segments of each active vertex
   */
  private LinkedList<Pair<DirectedVertex, List<EdgeSegment>>> topologicalEdgeSegmentList = new LinkedList<Pair<DirectedVertex, List<EdgeSegment>>>();

  /**
   * Constructor
   * 
   * @param groupId     generate id based on the group it resides in
   * @param parentGraph parent Graph we are a subset of
   * @param root        (initial) root of the subgraph
   */
  public ACyclicSubGraph(final IdGroupingToken groupId, DirectedGraph<V, E, ES> parentGraph, DirectedVertex root) {
    this.id = IdGenerator.generateId(groupId, ACyclicSubGraph.class);
    this.parentGraph = parentGraph;
    this.root = root;
    topologicalEdgeSegmentList.add(Pair.of(root, new ArrayList<EdgeSegment>()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedGraph<V, E, ES> getParentGraph() {
    return parentGraph;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getRootVertex() {
    return root;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addEdgeSegment(EdgeSegment edgeSegment) {
    return false;
  }

}
