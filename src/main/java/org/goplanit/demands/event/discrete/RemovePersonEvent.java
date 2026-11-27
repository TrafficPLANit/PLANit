package org.goplanit.demands.event.discrete;

import org.goplanit.demands.discrete.DiscreteDemandsModifier;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.event.EventImpl;

/**
 * Event for when a person has been removed from the discrete demands, with everything they do
 * <p>
 * It lets whatever keeps its own view of the demand, such as an index of persons by household or a count of persons
 * per zone, follow removals made elsewhere. The modifier fires it only when someone listens for it, so removing
 * millions of persons, as a down-sampling does, costs nothing extra when nobody does.
 * </p>
 *
 * @author markr
 */
public class RemovePersonEvent extends EventImpl implements DiscreteDemandsModificationEvent {

  /** event type fired off when a person has been removed */
  public static final DiscreteDemandsModifierEventType EVENT_TYPE =
      new DiscreteDemandsModifierEventType("DISCRETEDEMANDSMODIFIER.PERSON.REMOVE");

  /**
   * Constructor
   *
   * @param source        discrete demands modifier firing the event
   * @param removedPerson person that is removed
   */
  public RemovePersonEvent(DiscreteDemandsModifier source, Person removedPerson) {
    super(EVENT_TYPE, source, removedPerson);
  }

  /**
   * The removed person
   *
   * @return removed person
   */
  public Person getRemovedPerson() {
    return (Person) getContent()[0];
  }
}
