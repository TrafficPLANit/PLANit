package org.goplanit.graph.directed.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;

/**
 * Wrapper around break edge segment event
 * 
 * @author markr
 *
 */
public class BreakEdgeSegmentEvent extends EventImpl implements DirectedGraphModificationEvent {

  /** event type fired off when edge has been broken */
  public static final DirectedGraphModifierEventType EVENT_TYPE =
      new DirectedGraphModifierEventType("DIRECTEDGRAPHMODIFIER.EDGESEGMENT.BREAK");

  /**
   * constructor
   * 
   * @param source            of the event
   * @param vertexToBreakAt   vertex to break at
   * @param edgeSegmentNow    the new edge segment as a result of breaking at vertexToBreakAt
   * @param edgeSegmentBefore the edge segment that the new edge segment replaced (may be partially changed as it may
   *                          be repurposed for memory efficiency reasons, but the instance was the original before
   *                          breaking)
   */
  public BreakEdgeSegmentEvent(
      final DirectedGraphModifier source,
      final DirectedVertex vertexToBreakAt,
      EdgeSegment edgeSegmentNow,
      EdgeSegment edgeSegmentBefore) {
    super(EVENT_TYPE, source, new Object[] { vertexToBreakAt, edgeSegmentNow, edgeSegmentBefore });
  }

  /**
   * collect vertex to break at
   * 
   * @return vertex to break at
   */
  public DirectedVertex getVertexToBreakAt() {
    return (DirectedVertex) ((Object[]) getContent())[0];
  }

  /**
   * Collect broken edge segment
   * 
   * @return edgeSegment
   */
  public EdgeSegment getNewlyBrokenEdgeSegment() {
    return (EdgeSegment) ((Object[]) getContent())[1];
  }

  /**
   * Collect broken edge segment
   *
   * @return edgeSegment
   */
  public EdgeSegment getOriginalEdgeSegment() {
    return (EdgeSegment) ((Object[]) getContent())[2];
  }

}
