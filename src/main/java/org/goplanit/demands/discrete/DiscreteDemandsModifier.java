package org.goplanit.demands.discrete;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.demands.event.discrete.*;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

import java.util.Set;
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
   * @return trip that was removed
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
        if (scheduleElement instanceof TourImpl) {
          removeTour((TourImpl) scheduleElement, false);
        } else if (scheduleElement instanceof TripImpl) {
          removeTrip((Trip) scheduleElement);
        }
      }
    }
    // remove from container
    discreteDemands.getTours().remove(tour);

  }

  /**
   * Shift each person's whole schedule by a single random offset, deterministically per person and kept within the
   * allowed period boundaries. Elements that share a time bin stay together, the whole schedule simply moves.
   *
   * todo: does not use any events yet, it should
   * @param maxDeviationSeconds the maximum deviation allowed (+/- maxDeviationSeconds)
   * @param symmetric if true, shifts by [-max, +max]; if false, shifts strictly forward by [0, max]
   * @param minAllowedTimeSeconds the lower boundary (e.g., start of simulation period)
   * @param maxAllowedTimeSeconds the upper boundary (e.g., end of simulation period)
   */
  public void adjustPersonsScheduleRigidJitter(
      int maxDeviationSeconds, boolean symmetric, int minAllowedTimeSeconds, int maxAllowedTimeSeconds) {

    if (maxDeviationSeconds <= 0 || discreteDemands.getPersons().isEmpty()) {
      return;
    }

    discreteDemands.getPersons().forEach(person -> ScheduleJitter.applyRigidPersonJitter(
        person, maxDeviationSeconds, symmetric, minAllowedTimeSeconds, maxAllowedTimeSeconds));
  }

  /**
   * Shift a single person's whole schedule by a single random offset, see
   * {@link #adjustPersonsScheduleRigidJitter(int, boolean, int, int)}
   *
   * @param person the person whose schedule to jitter
   * @param maxDeviationSeconds the maximum deviation allowed (+/- maxDeviationSeconds)
   * @param symmetric if true, shifts by [-max, +max]; if false, shifts strictly forward by [0, max]
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   */
  public void adjustPersonScheduleRigidJitter(
      Person person, int maxDeviationSeconds, boolean symmetric, int minAllowedTimeSeconds,
      int maxAllowedTimeSeconds) {

    if (maxDeviationSeconds <= 0) {
      return;
    }
    ScheduleJitter.applyRigidPersonJitter(
        person, maxDeviationSeconds, symmetric, minAllowedTimeSeconds, maxAllowedTimeSeconds);
  }

  /**
   * Spread each person's schedule within the time bins it occupies, so that elements sharing a bin no longer carry
   * an identical time. Spreading is forward only, since spreading backwards would take elements out of their own
   * bin.
   *
   * todo: does not use any events yet, it should
   * @param spreadWidthSeconds width to spread over, being the bin length scaled by any configured ratio
   * @param minAllowedTimeSeconds the lower boundary (e.g., start of simulation period)
   * @param maxAllowedTimeSeconds the upper boundary (e.g., end of simulation period)
   */
  public void adjustPersonsScheduleSubBinJitter(
      int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds) {
    adjustPersonsScheduleSubBinJitter(
        spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds, null, 0);
  }

  /**
   * Spread each person's schedule within the time bins it occupies, giving the provided tours a minimum duration
   * afterwards, see {@link #adjustPersonsScheduleSubBinJitter(int, int, int)}
   *
   * todo: does not use any events yet, it should
   * @param spreadWidthSeconds width to spread over, being the bin length scaled by any configured ratio
   * @param minAllowedTimeSeconds the lower boundary (e.g., start of simulation period)
   * @param maxAllowedTimeSeconds the upper boundary (e.g., end of simulation period)
   * @param minDurationTours tours to give a minimum duration, typically those that start and end in the same time
   *                         bin, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   */
  public void adjustPersonsScheduleSubBinJitter(
      int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds, Set<Tour> minDurationTours,
      int minDurationSeconds) {

    if (spreadWidthSeconds <= 0 || discreteDemands.getPersons().isEmpty()) {
      return;
    }

    discreteDemands.getPersons().forEach(person -> ScheduleJitter.applySubBinJitter(
        person, spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds, minDurationTours,
        minDurationSeconds));
  }

  /**
   * Spread a single person's schedule within the time bins it occupies, see
   * {@link #adjustPersonsScheduleSubBinJitter(int, int, int)}
   *
   * @param person the person whose schedule to jitter
   * @param spreadWidthSeconds width to spread over, being the bin length scaled by any configured ratio
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   */
  public void adjustPersonScheduleSubBinJitter(
      Person person, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds) {
    adjustPersonScheduleSubBinJitter(
        person, spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds, null, 0);
  }

  /**
   * Spread a single person's schedule within the time bins it occupies, giving the provided tours a minimum
   * duration afterwards, see {@link #adjustPersonsScheduleSubBinJitter(int, int, int, Set, int)}
   *
   * @param person the person whose schedule to jitter
   * @param spreadWidthSeconds width to spread over, being the bin length scaled by any configured ratio
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minDurationTours tours to give a minimum duration, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   */
  public void adjustPersonScheduleSubBinJitter(
      Person person, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds,
      Set<Tour> minDurationTours, int minDurationSeconds) {
    ScheduleJitter.applySubBinJitter(
        person, spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds, minDurationTours,
        minDurationSeconds);
  }
}
