package org.goplanit.demands.event.discrete;

import org.goplanit.demands.discrete.DiscreteDemandsModifier;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.utils.event.EventImpl;

/**
 * Event for when a household has been removed from the discrete demands, after each of its persons
 * <p>
 * It lets whatever keeps its own view of the demand, such as households per zone, follow removals made elsewhere. It
 * comes after a {@link RemovePersonEvent} for each of the household's persons, so a listener sees the persons go
 * first. The modifier fires it only when someone listens for it, so it costs nothing extra when nobody does.
 * </p>
 *
 * @author markr
 */
public class RemoveHouseholdEvent extends EventImpl implements DiscreteDemandsModificationEvent {

  /** event type fired off when a household has been removed */
  public static final DiscreteDemandsModifierEventType EVENT_TYPE =
      new DiscreteDemandsModifierEventType("DISCRETEDEMANDSMODIFIER.HOUSEHOLD.REMOVE");

  /**
   * Constructor
   *
   * @param source           discrete demands modifier firing the event
   * @param removedHousehold household that is removed
   */
  public RemoveHouseholdEvent(DiscreteDemandsModifier source, Household removedHousehold) {
    super(EVENT_TYPE, source, removedHousehold);
  }

  /**
   * The removed household
   *
   * @return removed household
   */
  public Household getRemovedHousehold() {
    return (Household) getContent()[0];
  }
}
