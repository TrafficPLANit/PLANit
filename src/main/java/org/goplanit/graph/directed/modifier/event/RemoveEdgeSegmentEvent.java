package org.goplanit.graph.directed.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;

/**
 * Event for when an edge segment has been removed from a (sub) graph. While the listeners run, the segment is out of
 * the graph but still on its parent edge, so it can tell the vertices it ran between; it is taken off the edge after
 * <p>
 * Whatever refers to a segment has to follow its removal: the intersections it approaches, the movements entering or
 * leaving on it, the connectoids reached over it. A listener finds those by the segment itself, or by the vertices it
 * ran between, for example the connectoids at its downstream vertex that use it as an access link segment.
 * </p>
 * 
 * @author markr
 *
 */
public class RemoveEdgeSegmentEvent extends EventImpl implements DirectedGraphModificationEvent {

  /** event type fired off when sub graph edge segment has been removed */
  public static final GraphModifierEventType EVENT_TYPE =
      new DirectedGraphModifierEventType("DIRECTEDGRAPHMODIFIER.EDGESEGMENT.REMOVE");

  /**
   * Constructor
   * 
   * @param source             directed graph modifier firing the event
   * @param removedEdgeSegment edge segment that is removed
   */
  public RemoveEdgeSegmentEvent(DirectedGraphModifier source, EdgeSegment removedEdgeSegment) {
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
