package org.goplanit.graph.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;

/**
 * Event called after a portion (subgraph) of a graph has been removed
 * 
 * @author markr
 *
 */
public class RemoveSubGraphEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when sub graph vertex has been removed */
  public static final GraphModifierEventType EVENT_TYPE = new GraphModifierEventType("GRAPHMODIFIER.SUBGRAPH.REMOVE");

  /**
   * Constructor
   * 
   * @param source graph modifier firing the event
   */
  public RemoveSubGraphEvent(GraphModifier<?, ?> source) {
    super(EVENT_TYPE, source, null);
  }

}
