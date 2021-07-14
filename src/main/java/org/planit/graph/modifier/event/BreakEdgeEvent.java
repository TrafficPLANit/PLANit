package org.planit.graph.modifier.event;

import java.io.Serializable;

import org.djutils.event.EventType;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;

/**
 * Wrapper around break edge event fired by graph modifier
 * 
 * @author markr
 *
 */
public class BreakEdgeEvent extends org.djutils.event.Event {

  /**
   * Generated UID
   */
  private static final long serialVersionUID = -3141892625663948521L;

  /** event type fired off when edge has been broken */
  public static final EventType BREAK_EDGE = new EventType("GRAPHMODIFIER.BREAK_EDGE");

  /**
   * constructor
   * 
   * @param sourceId        of the event
   * @param vertexToBreakAt vertex to break at
   * @param aToBreak        edge broken now from vertex a to break
   * @param breakToB        edge broken now from break to b vertex
   */
  public BreakEdgeEvent(final Serializable sourceId, final Vertex vertexToBreakAt, Edge aToBreak, Edge breakToB) {
    super(BREAK_EDGE, sourceId, new Object[] { vertexToBreakAt, aToBreak, breakToB });
  }

  /**
   * collect vertex to break at
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
