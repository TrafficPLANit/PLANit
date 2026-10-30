package org.goplanit.demands.event.discrete;

import org.goplanit.demands.discrete.DiscreteDemandsModifier;
import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

/**
 * Event fired upon recreation of managed ids for discrete demand entities
 */
public class RecreatedDiscreteDemandsEntitiesManagedIdsEvent extends EventImpl implements GraphModificationEvent {

  /** event type fired off when managed id entities have been recreated */
  public static final DiscreteDemandsModifierEventType EVENT_TYPE =
      new DiscreteDemandsModifierEventType("DISCRETEDEMANDSMODIFIER.MANAGEDIDENTITIES.RECREATED");

  /**
   * Constructor
   *
   * @param source of the event
   * @param managedIdEntities that have been recreated in terms of their ids
   */
  public RecreatedDiscreteDemandsEntitiesManagedIdsEvent(
      final DiscreteDemandsModifier source, final ManagedIdEntities<? extends ManagedId> managedIdEntities) {
    super(EVENT_TYPE, source, new Object[] {managedIdEntities});
  }

  public ManagedIdEntities<? extends ManagedId> getManagedIdEntities(){
    return (ManagedIdEntities<? extends ManagedId>) getContent()[0];
  }

}
