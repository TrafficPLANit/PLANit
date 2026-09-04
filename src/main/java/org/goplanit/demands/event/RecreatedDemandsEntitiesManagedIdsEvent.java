package org.goplanit.demands.event;

import org.goplanit.demands.DemandsModifier;
import org.goplanit.demands.discrete.DiscreteDemandsModifier;
import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

/**
 * Event fired upon recreation of managed ids for demand entities
 */
public class RecreatedDemandsEntitiesManagedIdsEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when managed id entities have been recreated */
  public static final DemandsModifierEventType EVENT_TYPE =
      new DemandsModifierEventType("DEMANDSMODIFIER.MANAGEDIDENTITIES.RECREATED");

  /**
   * Constructor
   *
   * @param source of the event
   * @param managedIdEntities that have been recreated in terms of their ids
   */
  public RecreatedDemandsEntitiesManagedIdsEvent(
      final DemandsModifier source, final ManagedIdEntities<? extends ManagedId> managedIdEntities) {
    super(EVENT_TYPE, source, new Object[] {managedIdEntities});
  }

  public ManagedIdEntities<? extends ManagedId> getManagedIdEntities(){
    return (ManagedIdEntities<? extends ManagedId>) getContent()[0];
  }

}
