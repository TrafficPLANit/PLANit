package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.utils.mode.Mode;

import java.time.LocalTime;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utilities for person instances
 */
public class PersonUtils {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PersonUtils.class.getCanonicalName());

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
   * Verify each person's schedule is in chronological order and report those that are not. Inconsistent schedules
   * are reported only, not corrected, since they indicate a defect in the demand input rather than in PLANit.
   *
   * @param persons to check
   * @param dayAnchorTime time of day the schedules' day is cut at, may be null when no midnight crossing occurs
   * @return number of persons whose schedule is not in chronological order
   */
  public static int validateSchedulesChronological(Persons persons, LocalTime dayAnchorTime) {

    int offendingPersonCount = 0;
    for (var person : persons) {
      var schedule = person.getSchedule();
      if (schedule == null || schedule.isEmpty() || schedule.isChronological(dayAnchorTime)) {
        continue;
      }

      ++offendingPersonCount;
      LOGGER.warning(String.format(
          "Schedule of person (%s) is not in chronological order, for example a tour ending before one of its own " +
              "trips departs. This is not corrected, but the demand input is inconsistent",
          person.getIdsAsString()));
    }
    return offendingPersonCount;
  }
}
