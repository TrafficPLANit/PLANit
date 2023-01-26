package org.goplanit.graph.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.id.IdAble;

/**
 * Wrapper around break edge event
 * 
 * @author markr
 *
 */
public class BreakEdgeEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when edge has been broken */
  public static final GraphModifierEventType EVENT_TYPE = new GraphModifierEventType("GRAPHMODIFIER.BREAK_EDGE");

  /**
   * Constructor
   * 
   * @param source          of the event
   * @param vertexToBreakAt vertex to break at
   * @param aToBreak        edge broken now from vertex a to break
   * @param breakToB        edge broken now from break to b vertex
   */
  public BreakEdgeEvent(final GraphModifier<?, ?> source, final Vertex vertexToBreakAt, Edge aToBreak, Edge breakToB) {
    super(EVENT_TYPE, source, new Object[] { vertexToBreakAt, aToBreak, breakToB });
  }

  /**
   * Collect vertex to break at
   * 
   * @return vertex to break at
   */
  public Vertex getVertexToBreakAt() {
    return (Vertex) ((Object[]) getContent())[0];
  }

  /**
   * collect broken edge section from original vertex A to break vertex
   * 
   * @return edge
   */
  public Edge getEdgeFromVertexAToBreak() {
    return (Edge) ((Object[]) getContent())[1];
  }

  /**
   * collect broken edge section from break vertex to original vertex B
   * 
   * @return edge
   */
  public Edge getEdgeFromBreakToVertexB() {
    return (Edge) ((Object[]) getContent())[2];
  }

}
