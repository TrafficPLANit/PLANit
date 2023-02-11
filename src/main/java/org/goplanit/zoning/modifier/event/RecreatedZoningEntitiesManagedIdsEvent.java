package org.goplanit.zoning.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;

/**
 * Event fired upon recreation of managed ids for zoning in managed id containers
 */
public class RecreatedZoningEntitiesManagedIdsEvent extends EventImpl implements ZoningModificationEvent {

  /** event type fired off when managed id entities have been recreated */
  public static final DirectedGraphModifierEventType EVENT_TYPE = new DirectedGraphModifierEventType("ZONINGMODIFIER.MANAGEDIDENTITIES.RECREATED");

  /**
   * Constructor
   *
   * @param source of the event
   * @param managedIdEntities that have been recreated in terms of their ids
   */
  public RecreatedZoningEntitiesManagedIdsEvent(final ZoningModifier source, final ManagedIdEntities<? extends ManagedId> managedIdEntities) {
    super(EVENT_TYPE, source, new Object[] {managedIdEntities});
  }

  public ManagedIdEntities<? extends ManagedId> getManagedIdEntities(){
    return (ManagedIdEntities<? extends ManagedId>) getContent()[0];
  }

}
