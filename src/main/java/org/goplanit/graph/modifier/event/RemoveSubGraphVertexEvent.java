package org.goplanit.graph.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;

/**
 * Event for when a vertex has been removed from a (sub) graph
 * 
 * @author markr
 *
 */
public class RemoveSubGraphVertexEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when sub graph vertex has been removed */
  public static final GraphModifierEventType EVENT_TYPE = new GraphModifierEventType("GRAPHMODIFIER.SUBGRAPH.VERTEX.REMOVE");

  /**
   * Constructor
   * 
   * @param source        graph modifier firing the event
   * @param removedVertex vertex that is removed
   */
  public RemoveSubGraphVertexEvent(GraphModifier<?, ?> source, Vertex removedVertex) {
    super(EVENT_TYPE, source, removedVertex);
  }

  /**
   * The removed vertex
   * 
   * @return removed vertex
   */
  public Vertex getRemovedVertex() {
    return (Vertex) getContent()[0];
  }

}
