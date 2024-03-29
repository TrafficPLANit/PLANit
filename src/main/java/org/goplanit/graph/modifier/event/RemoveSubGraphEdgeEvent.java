package org.goplanit.graph.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;

/**
 * Event for when an edge has been removed from a (sub) graph
 * 
 * @author markr
 *
 */
public class RemoveSubGraphEdgeEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when sub graph edge has been removed */
  public static final GraphModifierEventType EVENT_TYPE = new GraphModifierEventType("GRAPHMODIFIER.SUBGRAPH.EDGE.REMOVE");

  /**
   * Constructor
   * 
   * @param source      graph modifier firing the event
   * @param removedEdge edge that is removed
   */
  public RemoveSubGraphEdgeEvent(GraphModifier<?, ?> source, Edge removedEdge) {
    super(EVENT_TYPE, source, removedEdge);
  }

  /**
   * The removed edge
   * 
   * @return removed edge
   */
  public Edge getRemovedEdge() {
    return (Edge) getContent()[0];
  }

}
