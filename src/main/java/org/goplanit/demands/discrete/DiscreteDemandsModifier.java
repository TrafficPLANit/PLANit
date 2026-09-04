package org.goplanit.demands.discrete;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.PersonUtils;
import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.demands.event.discrete.*;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;

import java.util.SplittableRandom;
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
   * For a given schedule shift the entire schedule with a given number of seconds
   * @param schedule to shift
   * @param offsetSeconds offset to apply
   */
  private static void shiftScheduleRecursive(ActivitySchedule schedule, int offsetSeconds) {
    for (ScheduleElement element : schedule) {
      // Shift start time (supported by both Tour and Trip)
      if (element.getStartTime() != null) {
        element.setStartTime(element.getStartTime().plusSeconds(offsetSeconds));
      }

      // If it's a Tour, it also has an end time that needs shifting
      if (element instanceof Tour) {
        Tour tour = (Tour) element;
        if (tour.getEndTime() != null) {
          tour.setEndTime(tour.getEndTime().plusSeconds(offsetSeconds));
        }
      }

      // Recursively shift nested schedules if they exist (e.g., sub-tours inside a tour)
      if (element.hasSchedule() && element.getSchedule() != null) {
        shiftScheduleRecursive(element.getSchedule(), offsetSeconds);
      }
    }
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
   * Applies uniform temporal jitter globally to a PLANit discrete demands container
   * with boundary snapping and deterministic reproducibility via SplittableRandom.
   *
   * todo: does not use any events yet, it should
   * @param maxDeviationSeconds the maximum deviation allowed (+/- maxDeviationSeconds)
   * @param symmetric if true, shifts by [-max, +max]; if false, shifts strictly forward by [0, max]
   * @param minAllowedTimeSeconds the lower boundary (e.g., start of simulation period)
   * @param maxAllowedTimeSeconds the upper boundary (e.g., end of simulation period)
   */
  public void adjustPersonsScheduleUniformJitter(
      int maxDeviationSeconds, boolean symmetric, int minAllowedTimeSeconds, int maxAllowedTimeSeconds) {

    if (maxDeviationSeconds <= 0 || discreteDemands.getPersons().isEmpty()) {
      return;
    }

    discreteDemands.getPersons().forEach(person ->
        adjustPersonScheduleUniformJitter(
            person, maxDeviationSeconds, symmetric, minAllowedTimeSeconds, maxAllowedTimeSeconds)
    );
  }

  /**
   * Person-level method that applies uniform temporal jitter deterministically
   * to a single person's schedule, clamping within allowed period boundaries.
   *
   * @param person the person whose schedule to jitter
   * @param maxDeviationSeconds the maximum deviation allowed (+/- maxDeviationSeconds)
   * @param symmetric if true, shifts by [-max, +max]; if false, shifts strictly forward by [0, max]
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   */
  public void adjustPersonScheduleUniformJitter(
      Person person,
      int maxDeviationSeconds,
      boolean symmetric,
      int minAllowedTimeSeconds,
      int maxAllowedTimeSeconds) {

    if (maxDeviationSeconds <= 0 || person == null || person.getSchedule() == null || person.getSchedule().isEmpty()) {
      return;
    }

    var schedule = person.getSchedule();
    SplittableRandom rng = new SplittableRandom(PersonUtils.generatePersonSeed(person));

    // Draw uniform offset across the full window [0, totalWindow] and center it around 0
    int offsetSeconds;
    if (symmetric) {
      // Symmetric range: [-maxDeviationSeconds, +maxDeviationSeconds]
      double totalWindowSeconds = maxDeviationSeconds * 2.0;
      offsetSeconds = (int) Math.round((rng.nextDouble() * totalWindowSeconds) - maxDeviationSeconds);
    } else {
      // Unidirectional positive-only spread: [0, maxDeviationSeconds]
      offsetSeconds = (int) Math.round(rng.nextDouble() * maxDeviationSeconds);
    }
    if (offsetSeconds == 0) {
      return;
    }

    //CLAMPING
    {
      //  Determine schedule boundaries using the first element's start time for clamping
      // lower bound clamping
      var first = schedule.getFirst();
      if (first != null && first.getStartTime() != null) {
        int earliestSeconds = first.getStartTime().toSecondOfDay();
        if (earliestSeconds + offsetSeconds < minAllowedTimeSeconds) {
          offsetSeconds = minAllowedTimeSeconds - earliestSeconds;
        }
      }

      // Upper bound clamping
      var last = schedule.getLast(false /* not flattened, because we're after end time of last tour */);
      if (last == null ||  !(last instanceof Tour) || ((Tour) last).getEndTime() == null) {
        LOGGER.severe(String.format("Schedule of person (%s) has invalid top level last tour, unable to apply jitter",
            person.getIdsAsString()));
        return;
      }
      var latestTime = ((Tour) last).getEndTime();
      int latestSeconds = latestTime.toSecondOfDay();
      if (latestSeconds + offsetSeconds > maxAllowedTimeSeconds) {
        offsetSeconds = maxAllowedTimeSeconds - latestSeconds;
      }
    }

    // Apply the valid offset recursively across the schedule elements and sort
    if (offsetSeconds != 0) {
      shiftScheduleRecursive(schedule, offsetSeconds);
    }
  }
}
