package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.utils.mode.Mode;

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
}
