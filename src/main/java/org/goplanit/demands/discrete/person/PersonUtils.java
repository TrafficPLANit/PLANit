package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.utils.mode.Mode;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for person instances
 */
public class PersonUtils {

  /**
   * Find the persons which have one or more schedule elements containing one of the provided modes
   * @param persons to check
   * @param modes modes to check
   * @return subset of persons found
   */
  public static Set<Person> findPersonsWithScheduleElementsContaining(Persons persons, Set<Mode> modes) {
    if (modes == null || modes.isEmpty()) {
      return Set.of();
    }

    return persons.stream()
        .filter(p -> p.getSchedule()!=null)
        .filter(p -> p.getSchedule().testNested(se -> se instanceof TripImpl && modes.contains(((Trip)se).getMode())))
        .collect(Collectors.toSet());
  }

  /**
   * Generates a deterministic seed based on person ID and structural characteristics
   * to ensure reproducibility across runs.
   */
  public static long generatePersonSeed(Person person) {
    return person.generateIdBasedRandomSeed();
  }

  /**
   * Find the persons whose schedule is not in chronological order, e.g. a tour ending before one of its own trips
   * departs. Such a schedule indicates a defect in the demand input rather than in PLANit, so what is done with the
   * persons found is left to the caller
   *
   * @param persons to check
   * @param dayAnchorTime time of day the schedules' day is cut at, may be null when no midnight crossing occurs
   * @return persons whose schedule is not in chronological order, in the order they were encountered
   */
  public static List<Person> findPersonsWithNonChronologicalSchedule(Persons persons, LocalTime dayAnchorTime) {
    return persons.stream()
        .filter(p -> p.getSchedule() != null && !p.getSchedule().isEmpty())
        .filter(p -> !p.getSchedule().isChronological(dayAnchorTime))
        .collect(Collectors.toList());
  }
}
