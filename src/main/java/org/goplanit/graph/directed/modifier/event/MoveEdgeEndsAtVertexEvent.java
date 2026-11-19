package org.goplanit.graph.directed.modifier.event;

import java.util.List;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;

/**
 * Fired once when the ends at a vertex of one or more edges have moved to another vertex in one call, after they moved
 * <p>
 * What refers to the vertex over these edges from outside the graph, such as a transfer connectoid reached over their
 * segments, can follow them to the new vertex. Naming every edge moved in the call, rather than one event per edge, lets
 * it move as a whole when all its edges moved.
 * </p>
 *
 * @author markr
 */
public class MoveEdgeEndsAtVertexEvent extends EventImpl implements DirectedGraphModificationEvent {

  /** event type fired when the ends at a vertex of edges have moved to another vertex */
  public static final DirectedGraphModifierEventType EVENT_TYPE =
      new DirectedGraphModifierEventType("DIRECTEDGRAPHMODIFIER.EDGE.MOVE_ENDS_AT_VERTEX");

  /**
   * Constructor
   *
   * @param source of the event
   * @param edges whose ends moved
   * @param previousEndVertex the vertex the ends moved away from
   * @param newEndVertex the vertex the ends moved to
   */
  public MoveEdgeEndsAtVertexEvent(final DirectedGraphModifier<?, ?, ?> source, final List<? extends DirectedEdge> edges,
      final DirectedVertex previousEndVertex, final DirectedVertex newEndVertex) {
    super(EVENT_TYPE, source, new Object[] { List.copyOf(edges), previousEndVertex, newEndVertex });
  }

  /**
   * The edges whose ends moved
   *
   * @return edges, in the order given to the move
   */
  @SuppressWarnings("unchecked")
  public List<DirectedEdge> getEdges() {
    return (List<DirectedEdge>) ((Object[]) getContent())[0];
  }

  /**
   * The vertex the ends moved away from
   *
   * @return previous end vertex
   */
  public DirectedVertex getPreviousEndVertex() {
    return (DirectedVertex) ((Object[]) getContent())[1];
  }

  /**
   * The vertex the ends moved to
   *
   * @return new end vertex
   */
  public DirectedVertex getNewEndVertex() {
    return (DirectedVertex) ((Object[]) getContent())[2];
  }
}
