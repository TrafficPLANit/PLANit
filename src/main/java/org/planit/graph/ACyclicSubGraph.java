package org.planit.graph;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * An acyclic sub graph contains a subset of the full graph without cycles. The active subset of the graph is tracked by explicitly registering edge segments. Edge segments are by
 * definition directed.
 * 
 * Whenever edge segments are added it is verified that no cycles are created. Also each edge segment that is added must connect to the existing subgraph's contents
 * 
 * @author markr
 *
 */
public class ACyclicSubGraph {

  /**
   * The id of this acyclic sub graph
   */
  private final long id;

  /**
   * The parent graph where this is an acyclic subgraph of
   */
  GraphImpl parentGraph;

  /**
   * root of the sub graph
   */
  Vertex root;

  /**
   * Constructor
   * 
   * @param groupId     generate id based on the group it resides in
   * @param parentGraph parent Graph we are a subset of
   * @param root        (initial) root of the subgraph
   */
  public ACyclicSubGraph(final IdGroupingToken groupId, GraphImpl parentGraph, Vertex root) {
    this.id = IdGenerator.generateId(groupId, ACyclicSubGraph.class);
    this.parentGraph = parentGraph;
    this.root = root;
  }

  public void addEdgeSegment(EdgeSegment edge) {

  }

}
