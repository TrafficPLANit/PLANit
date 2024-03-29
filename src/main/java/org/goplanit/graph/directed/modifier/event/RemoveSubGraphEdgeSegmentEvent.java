package org.goplanit.graph.directed.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;

/**
 * Event for when an edge segment has been removed from a (sub) graph
 * 
 * @author markr
 *
 */
public class RemoveSubGraphEdgeSegmentEvent extends EventImpl implements DirectedGraphModificationEvent {

  /** event type fired off when sub graph edge segment has been removed */
  public static final GraphModifierEventType EVENT_TYPE = new DirectedGraphModifierEventType("DIRECTEDGRAPHMODIFIER.SUBGRAPH.EDGESEGMENT.REMOVE");

  /**
   * Constructor
   * 
   * @param source             directed graph modifier firing the event
   * @param removedEdgeSegment edge segment that is removed
   */
  public RemoveSubGraphEdgeSegmentEvent(DirectedGraphModifier source, EdgeSegment removedEdgeSegment) {
    super(EVENT_TYPE, source, removedEdgeSegment);
  }

  /**
   * The removed edge segment
   * 
   * @return removed edge segment
   */
  public EdgeSegment getRemovedEdgeSegment() {
    return (EdgeSegment) getContent()[0];
  }

}
