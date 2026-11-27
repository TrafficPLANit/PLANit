package org.goplanit.test.pricing;

import org.goplanit.pricing.PricingTimeBandEntryImpl;
import org.goplanit.utils.pricing.HolidayInclusion;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.goplanit.test.pricing.PricingTestUtils.WEEKDAYS;
import static org.goplanit.test.pricing.PricingTestUtils.at;
import static org.goplanit.test.pricing.PricingTestUtils.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of when a time band entry applies, and when two entries overlap: on which days, in which minutes of the day,
 * past midnight, and on holidays or not
 *
 * @author markr
 */
public class PricingTimeBandEntryTest {

  /**
   * A band with an entry for weekdays 06:30 to 09:30 and one for weekdays 16:00 to 19:00 applies at 07:00 and at 17:00
   * on a Tuesday, and not at 12:00, nor at 07:00 on a Saturday ("Two peaks in one band")
   */
  @Test
  public void twoPeaksInOneBand() {
    var band = List.of(entry(WEEKDAYS, 390, 180), entry(WEEKDAYS, 960, 180));
    assertTrue(band.stream().anyMatch(entry -> entry.covers(at(DayOfWeek.TUESDAY, 7, 0))));
    assertTrue(band.stream().anyMatch(entry -> entry.covers(at(DayOfWeek.TUESDAY, 17, 0))));
    assertFalse(band.stream().anyMatch(entry -> entry.covers(at(DayOfWeek.TUESDAY, 12, 0))));
    assertFalse(band.stream().anyMatch(entry -> entry.covers(at(DayOfWeek.SATURDAY, 7, 0))));
  }

  /** An interval holds its first minute and not the minute it ends at */
  @Test
  public void intervalHoldsItsStartAndNotItsEnd() {
    var morning = entry(WEEKDAYS, 390, 180);
    assertTrue(morning.covers(at(DayOfWeek.MONDAY, 6, 30)));
    assertTrue(morning.covers(at(DayOfWeek.MONDAY, 9, 29)));
    assertFalse(morning.covers(at(DayOfWeek.MONDAY, 9, 30)));
  }

  /**
   * An entry for every day starting at 21:00 and lasting 8 hours applies at 23:00 on a Monday and at 04:00 on the
   * Tuesday after, not at 05:00; on Sunday night it runs into Monday morning ("Entry past midnight")
   */
  @Test
  public void entryPastMidnight() {
    var night = new PricingTimeBandEntryImpl(Set.of(), null, null, 1260, 480, ChronoUnit.HOURS);
    assertTrue(night.covers(at(DayOfWeek.MONDAY, 23, 0)));
    assertTrue(night.covers(at(DayOfWeek.TUESDAY, 4, 0)));
    assertFalse(night.covers(at(DayOfWeek.TUESDAY, 5, 0)));
    assertTrue(night.covers(at(DayOfWeek.MONDAY, 2, 0)));

    var fridayNight = entry(Set.of(DayOfWeek.FRIDAY), 1260, 480);
    assertTrue(fridayNight.covers(at(DayOfWeek.SATURDAY, 4, 0)));
    assertFalse(fridayNight.covers(at(DayOfWeek.FRIDAY, 4, 0)));
  }

  /**
   * Entries for weekdays 07:00 to 10:00 and 09:00 to 18:00 overlap; entries meeting at 10:00, or on different days, do
   * not ("Overlapping bands")
   */
  @Test
  public void entriesSharingADayAndMinutesOverlap() {
    var morning = entry(WEEKDAYS, 420, 180);
    assertTrue(morning.overlaps(entry(WEEKDAYS, 540, 540)));
    assertTrue(entry(WEEKDAYS, 540, 540).overlaps(morning));
    assertFalse(morning.overlaps(entry(WEEKDAYS, 600, 480)));
    assertFalse(morning.overlaps(entry(Set.of(DayOfWeek.SATURDAY), 420, 180)));
    assertTrue(morning.overlaps(entry(Set.of(), 480, 60)));
  }

  /**
   * An entry past midnight is split into its part on the start day and its part on the next day: Monday 21:00 for
   * 8 hours overlaps Tuesday 03:00 to 04:00, not Monday 03:00 to 04:00
   */
  @Test
  public void entryPastMidnightOverlapsOnTheNextDay() {
    var mondayNight = entry(Set.of(DayOfWeek.MONDAY), 1260, 480);
    assertTrue(mondayNight.overlaps(entry(Set.of(DayOfWeek.TUESDAY), 180, 60)));
    assertFalse(mondayNight.overlaps(entry(Set.of(DayOfWeek.MONDAY), 180, 60)));
  }

  /**
   * Weekdays 07:00 to 10:00 with public holidays excluded and public holidays only at the same times do not overlap,
   * nor do they for school holidays; left out, a holiday setting behaves as included, overlapping both ("Holiday
   * entries apart")
   */
  @Test
  public void holidayEntriesApart() {
    var excluded = entry(WEEKDAYS, HolidayInclusion.EXCLUDED, 420, 180);
    var only = entry(Set.of(), HolidayInclusion.ONLY, 420, 180);
    assertFalse(excluded.overlaps(only));
    assertFalse(only.overlaps(excluded));
    assertTrue(entry(WEEKDAYS, 420, 180).overlaps(only));
    assertTrue(entry(WEEKDAYS, 420, 180).overlaps(excluded));

    var schoolExcluded = new PricingTimeBandEntryImpl(WEEKDAYS, null, HolidayInclusion.EXCLUDED, 420, 180);
    var schoolOnly = new PricingTimeBandEntryImpl(WEEKDAYS, null, HolidayInclusion.ONLY, 420, 180);
    assertFalse(schoolExcluded.overlaps(schoolOnly));
  }

  /**
   * An entry excluding public holidays does not apply on one, an entry for public holidays only applies on nothing
   * else, and an entry leaving them out applies on both
   */
  @Test
  public void holidaySettingsDecideWhetherAMomentIsCovered() {
    var publicHoliday = TimeInWeekWithDayType.of(DayOfWeek.MONDAY, 480, true, false);
    var workingDay = at(DayOfWeek.MONDAY, 8, 0);
    var excluded = entry(WEEKDAYS, HolidayInclusion.EXCLUDED, 420, 180);
    var only = entry(Set.of(), HolidayInclusion.ONLY, 420, 180);
    var leftOut = entry(WEEKDAYS, 420, 180);

    assertFalse(excluded.covers(publicHoliday));
    assertTrue(excluded.covers(workingDay));
    assertTrue(only.covers(publicHoliday));
    assertFalse(only.covers(workingDay));
    assertTrue(leftOut.covers(publicHoliday));
    assertTrue(leftOut.covers(workingDay));
  }

  /** An entry keeps what it is given, days left out given as none and holidays left out as included */
  @Test
  public void entryKeepsWhatItIsGiven() {
    var night = new PricingTimeBandEntryImpl(Set.of(), HolidayInclusion.ONLY, null, 1260, 480, ChronoUnit.HOURS);
    assertTrue(night.getDays().isEmpty());
    assertEquals(HolidayInclusion.ONLY, night.getPublicHolidays());
    assertEquals(HolidayInclusion.INCLUDED, night.getSchoolHolidays());
    assertEquals(1260, night.getStartMinuteOfDay());
    assertEquals(480, night.getDurationMinutes());
    assertEquals(ChronoUnit.HOURS, night.getPersistTemporalUnit());
  }

  /**
   * An entry made without a unit to persist in takes the largest of days, hours and minutes dividing its start and
   * duration: 07:00 to 10:00 in hours, 06:30 to 09:30 in minutes, a whole day in days
   */
  @Test
  public void entryMadeWithoutAUnitPersistsInTheLargestDividingIt() {
    assertEquals(ChronoUnit.HOURS, entry(WEEKDAYS, 420, 180).getPersistTemporalUnit());
    assertEquals(ChronoUnit.MINUTES, entry(WEEKDAYS, 390, 180).getPersistTemporalUnit());
    assertEquals(ChronoUnit.DAYS, entry(Set.of(), 0, 1440).getPersistTemporalUnit());
  }

  /**
   * A unit to persist in that does not divide the start and duration is refused, when made and when set: 06:30 to
   * 09:30 cannot be written in hours; setting a unit that divides them leaves when the entry applies as it was ("Unit
   * to persist in that does not divide the time")
   */
  @Test
  public void unitToPersistInThatDoesNotDivideTheTime() {
    assertThrows(IllegalArgumentException.class,
        () -> new PricingTimeBandEntryImpl(WEEKDAYS, null, null, 390, 180, ChronoUnit.HOURS));
    var morning = entry(WEEKDAYS, 390, 180);
    assertThrows(IllegalArgumentException.class, () -> morning.setPersistTemporalUnit(ChronoUnit.HOURS));
    assertEquals(ChronoUnit.MINUTES, morning.getPersistTemporalUnit());

    var peak = entry(WEEKDAYS, 420, 180);
    peak.setPersistTemporalUnit(ChronoUnit.MINUTES);
    assertEquals(ChronoUnit.MINUTES, peak.getPersistTemporalUnit());
    assertTrue(peak.covers(at(DayOfWeek.MONDAY, 7, 0)));
    assertFalse(peak.covers(at(DayOfWeek.MONDAY, 10, 0)));
  }

  /**
   * An entry lasting longer than a day or nothing, starting a day or more after midnight, or to persist in years is
   * refused when made; a whole day is not
   */
  @Test
  public void entryThatMakesNoSenseIsRefused() {
    assertThrows(IllegalArgumentException.class,
        () -> new PricingTimeBandEntryImpl(WEEKDAYS, null, null, 0, 60, ChronoUnit.YEARS));
    assertThrows(IllegalArgumentException.class, () -> new PricingTimeBandEntryImpl(WEEKDAYS, null, null, 0, 1500));
    assertThrows(IllegalArgumentException.class, () -> new PricingTimeBandEntryImpl(WEEKDAYS, null, null, 1440, 60));
    assertThrows(IllegalArgumentException.class, () -> new PricingTimeBandEntryImpl(WEEKDAYS, null, null, 0, 0));
    var wholeDay = new PricingTimeBandEntryImpl(Set.of(DayOfWeek.SUNDAY), null, null, 0, 1440);
    assertTrue(wholeDay.covers(at(DayOfWeek.SUNDAY, 23, 59)));
    assertFalse(wholeDay.covers(at(DayOfWeek.MONDAY, 0, 0)));
  }
}
