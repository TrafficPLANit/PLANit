package org.goplanit.graph.directed.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

/**
 * Event fired upon recreation of managed ids for directed graph entities
 */
public class RecreatedDirectedGraphEntitiesManagedIdsEvent extends EventImpl implements DirectedGraphModificationEvent {

  /** event type fired off when managed id entities have been recreated */
  public static final DirectedGraphModifierEventType EVENT_TYPE = new DirectedGraphModifierEventType("DIRECTEDGRAPHMODIFIER.MANAGEDIDENTITIES.RECREATED");

  /**
   * Constructor
   *
   * @param source of the event
   * @param managedIdEntities that have been recreated in terms of their ids
   */
  public RecreatedDirectedGraphEntitiesManagedIdsEvent(final DirectedGraphModifier source, final ManagedIdEntities<? extends ManagedId> managedIdEntities) {
    super(EVENT_TYPE, source, new Object[] {managedIdEntities});
  }

  public ManagedIdEntities<? extends ManagedId> getManagedIdEntities(){
    return (ManagedIdEntities<? extends ManagedId>) getContent()[0];
  }

}
