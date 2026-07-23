package org.goplanit.demands.discrete;

import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.event.discrete.*;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

import java.util.logging.Logger;

/**
 * Modifier for DiscreteDemands. To be used for complex changes that affect more than a single aspects of the
 * DiscreteDemands instance
 *
 */
public class DiscreteDemandsModifier extends EventProducerImpl implements DiscreteDemandsModifierEventProducer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(DiscreteDemandsModifier.class.getCanonicalName());

  /** parent discreteDemands */
  private final DiscreteDemands discreteDemands;

  /**
   * Recreate managed ids and fire #RecreatedDiscreteDemandsEntitiesManagedIdsEvent event upon completion
   *
   * @param entities to recreate ids for
   * @param <T> type
   */
  private <T extends ManagedIdEntities<? extends ManagedId>> void recreateManagedEntitiesIdsFor(T entities) {
    entities.recreateIds(true);
    fireEvent(new RecreatedDiscreteDemandsEntitiesManagedIdsEvent(this, entities));
  }

  /**
   * {@inheritDoc}
   * <p>
   *   make public so derived classes can access it as well
   * </p>
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((DiscreteDemandsModifierListener) eventListener).onDiscreteDemandsModificationEvent(
        (DiscreteDemandsModificationEvent) event);
  }

  /**
   * Constructor
   * @param discreteDemands parent demands to perform modifications on
   */
  public DiscreteDemandsModifier(DiscreteDemands discreteDemands){
    this.discreteDemands = discreteDemands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    recreateManagedEntitiesIdsFor(discreteDemands.getHouseholds());
    recreateManagedEntitiesIdsFor(discreteDemands.getPersons());
    recreateManagedEntitiesIdsFor(discreteDemands.getTours());
    recreateManagedEntitiesIdsFor(discreteDemands.getTrips());
  }

  /**
   * Reset modifier
   */
  public void reset() {
    super.removeAllListeners();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(DiscreteDemandsModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(DiscreteDemandsModifierListener listener, DiscreteDemandsModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(DiscreteDemandsModifierListener listener, DiscreteDemandsModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(DiscreteDemandsModifierListener listener) {
    super.removeListener(listener);
  }

  /**
   * Trip to remove,
   * todo: does not use any events yet, it should
   *
   * @param trip to remove
   */
  public Trip removeTrip(Trip trip) {
    return discreteDemands.getTrips().remove(trip);
  }

  /**
   * Tour to remove, if it has trips, those trips are also removed
   * todo: does not use any events yet, it should
   *
   * @param tour to remove
   * @param removeParent flag to indicate if the tour to remove is a sub-tour (has a parent), whether we remove the
   *                     parent as well. If true, the entire chain of tours for the person is removed, making it a
   *                     person without any tours.
   */
  public void removeTour(Tour tour, boolean removeParent) {
    var person = tour.getPerson();

    if(tour.hasParentTour() && removeParent){
      removeTour(tour.getParentTour(), removeParent);
      return;
    }

    // if on top level, remove it from the person's schedule
    if(person.getSchedule() != null) {
      person.getSchedule().remove(tour);
    }

    // remove dependents
    if(tour.hasSchedule()) {
      for (var scheduleElement : tour.getSchedule()) {
        if (scheduleElement instanceof Tour) {
          removeTour((Tour) scheduleElement, false);
        } else if (scheduleElement instanceof Trip) {
          removeTrip((Trip) scheduleElement);
        }
      }
    }
    // remove from container
    discreteDemands.getTours().remove(tour);

  }
}
