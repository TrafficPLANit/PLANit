package org.goplanit.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

/**
 * Event fired upon recreation of managed ids for graph entities
 */
public class RecreatedGraphEntitiesManagedIdsEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when managed id entities have been recreated */
  public static final DirectedGraphModifierEventType EVENT_TYPE = new DirectedGraphModifierEventType("GRAPHMODIFIER.MANAGEDIDENTITIES.RECREATED");

  /**
   * Constructor
   *
   * @param source of the event
   * @param managedIdEntities that have been recreated in terms of their ids
   */
  public RecreatedGraphEntitiesManagedIdsEvent(final GraphModifier source, final ManagedIdEntities<? extends ManagedId> managedIdEntities) {
    super(EVENT_TYPE, source, new Object[] {managedIdEntities});
  }

  public ManagedIdEntities<? extends ManagedId> getManagedIdEntities(){
    return (ManagedIdEntities<? extends ManagedId>) getContent()[0];
  }

}
