package org.goplanit.pricing;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.binning.Bin;
import org.goplanit.utils.pricing.HolidayInclusion;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.goplanit.utils.pricing.PricingTimeBandEntry;

/**
 * When a time band applies: on some days, on holidays or not, in one interval of the day of at most a day, which may
 * run past midnight
 * <p>
 * For example a night band from 21:00 for 8 hours on every day applies at 23:00 on a Monday and at 04:00 on the Tuesday
 * after. Whether it applies at a moment, and whether it overlaps an entry of another band, are both read from its day
 * intervals: the minutes it covers on each day it starts, and the part past midnight on the day after, each a range of
 * minutes from midnight that holds its start and not its end. An entry cannot change when it applies once made, so its
 * day intervals are worked out once, when it is made. The unit it is written in, such as hours for 21:00 for 8 hours,
 * is kept apart, only for writing it back as read. An entry that makes no sense on its own, starting outside the day,
 * lasting no time or longer than a day, or with a unit to write it in that does not divide its time, is refused.
 * </p>
 *
 * @author markr
 */
public class PricingTimeBandEntryImpl implements PricingTimeBandEntry {

  /** generated UID */
  private static final long serialVersionUID = 4527843069236118751L;

  /** the minutes in a day */
  private static final int MINUTES_PER_DAY = 1440;

  /** the units an entry may be written in, largest first */
  private static final List<ChronoUnit> UNITS = List.of(ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES);

  /**
   * The largest unit an entry may be written in that divides its start and its duration, so it is written as briefly
   * as it can be: 07:00 for 3 hours in hours, 06:30 for 3 hours in minutes
   *
   * @param startMinuteOfDay of the interval
   * @param durationMinutes of the interval
   * @return the unit, minutes when nothing larger divides both
   */
  private static ChronoUnit findLargestUnitDividing(int startMinuteOfDay, int durationMinutes) {
    return UNITS.stream()
        .filter(unit -> startMinuteOfDay % unit.getDuration().toMinutes() == 0
            && durationMinutes % unit.getDuration().toMinutes() == 0)
        .findFirst()
        .orElse(ChronoUnit.MINUTES);
  }

  /** the days it starts on, empty for every day */
  private final Set<DayOfWeek> days;

  /** whether public holidays count, included when not given */
  private final HolidayInclusion publicHolidays;

  /** whether school holidays count, included when not given */
  private final HolidayInclusion schoolHolidays;

  /** the minute of the day it starts at */
  private final int startMinuteOfDay;

  /** how many minutes it lasts */
  private final int durationMinutes;

  /** the minutes covered on each day, worked out when made, see {@link #findDayIntervals()} */
  private final List<Pair<DayOfWeek, Bin<Integer>>> dayIntervals;

  /** the unit it is written in */
  private ChronoUnit persistTemporalUnit;

  /**
   * The minutes the entry covers on each day, by the day: on each day it starts, from its start to its end or
   * midnight, and on the day after, from midnight to its end when it runs past midnight. Every day when it names none
   *
   * @return each day with the minutes covered on it, from midnight, start held and end not
   */
  private List<Pair<DayOfWeek, Bin<Integer>>> findDayIntervals() {
    int end = startMinuteOfDay + durationMinutes;
    var startDays = days.isEmpty() ? EnumSet.allOf(DayOfWeek.class) : days;
    var intervals = new ArrayList<Pair<DayOfWeek, Bin<Integer>>>(2 * startDays.size());
    for (var day : startDays) {
      intervals.add(
          Pair.of(
              day,
              Bin.of(
                  startMinuteOfDay, Math.min(end, MINUTES_PER_DAY), true, false, null)));
      if (end > MINUTES_PER_DAY) {
        intervals.add(
            Pair.of(
                day.plus(1),
                Bin.of(
                    0, end - MINUTES_PER_DAY, true, false, null)));
      }
    }
    return intervals;
  }

  /**
   * Whether a moment's day meets both holiday settings
   *
   * @param moment to check
   * @return true when it does
   */
  private boolean isMetBy(TimeInWeekWithDayType moment) {
    return publicHolidays.isAgreeWith(moment.isPublicHoliday()) && schoolHolidays.isAgreeWith(moment.isSchoolHoliday());
  }

  /**
   * Refuse a unit to write the entry in that is not minutes, hours or days, or that does not divide its start and its
   * duration, as the entry could not be written in it
   *
   * @param unit to check
   * @throws IllegalArgumentException when it is refused
   */
  private void checkPersistTemporalUnit(ChronoUnit unit) {
    ExceptionUtils.throwIllegalArgumentIf(!UNITS.contains(unit),
        "Time band entry unit %s refused, it is minutes, hours or days", unit);
    long unitMinutes = unit.getDuration().toMinutes();
    ExceptionUtils.throwIllegalArgumentIf(
        startMinuteOfDay % unitMinutes != 0 || durationMinutes % unitMinutes != 0,
        "Time band entry unit %s refused, it does not divide its start at minute %d and its %d minutes", unit,
        startMinuteOfDay, durationMinutes);
  }

  /**
   * Constructor, written in the largest unit dividing its start and duration
   *
   * @param days it starts on, empty for every day
   * @param publicHolidays whether public holidays count, null for included, as when not given
   * @param schoolHolidays whether school holidays count, null for included, as when not given
   * @param startMinuteOfDay the minute of the day it starts at, 0 to 1439
   * @param durationMinutes how many minutes it lasts, at least 1 and at most a day
   * @throws IllegalArgumentException when it starts outside the day, or lasts no time or longer than a day
   */
  public PricingTimeBandEntryImpl(
      Collection<DayOfWeek> days,
      HolidayInclusion publicHolidays,
      HolidayInclusion schoolHolidays,
      int startMinuteOfDay,
      int durationMinutes) {
    this(days, publicHolidays, schoolHolidays, startMinuteOfDay, durationMinutes,
        findLargestUnitDividing(startMinuteOfDay, durationMinutes));
  }

  /**
   * Constructor
   *
   * @param days it starts on, empty for every day
   * @param publicHolidays whether public holidays count, null for included, as when not given
   * @param schoolHolidays whether school holidays count, null for included, as when not given
   * @param startMinuteOfDay the minute of the day it starts at, 0 to 1439
   * @param durationMinutes how many minutes it lasts, at least 1 and at most a day
   * @param persistTemporalUnit the unit it is written in: minutes, hours or days
   * @throws IllegalArgumentException when it starts outside the day, lasts no time or longer than a day, or the unit is
   *     not minutes, hours or days or does not divide its start and duration
   */
  public PricingTimeBandEntryImpl(
      Collection<DayOfWeek> days,
      HolidayInclusion publicHolidays,
      HolidayInclusion schoolHolidays,
      int startMinuteOfDay,
      int durationMinutes,
      ChronoUnit persistTemporalUnit) {
    ExceptionUtils.throwIllegalArgumentIf(startMinuteOfDay < 0 || startMinuteOfDay >= MINUTES_PER_DAY,
        "Time band entry starting at minute %d refused, it starts within the day", startMinuteOfDay);
    ExceptionUtils.throwIllegalArgumentIf(durationMinutes < 1 || durationMinutes > MINUTES_PER_DAY,
        "Time band entry of %d minutes refused, it lasts at least a minute and at most a day", durationMinutes);

    this.days = days.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(days);
    this.publicHolidays = publicHolidays == null ? HolidayInclusion.INCLUDED : publicHolidays;
    this.schoolHolidays = schoolHolidays == null ? HolidayInclusion.INCLUDED : schoolHolidays;
    this.startMinuteOfDay = startMinuteOfDay;
    this.durationMinutes = durationMinutes;
    this.dayIntervals = findDayIntervals();
    setPersistTemporalUnit(persistTemporalUnit);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<DayOfWeek> getDays() {
    return Collections.unmodifiableSet(days);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HolidayInclusion getPublicHolidays() {
    return publicHolidays;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HolidayInclusion getSchoolHolidays() {
    return schoolHolidays;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getStartMinuteOfDay() {
    return startMinuteOfDay;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getDurationMinutes() {
    return durationMinutes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ChronoUnit getPersistTemporalUnit() {
    return persistTemporalUnit;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The day intervals stay as they are, as the unit is not part of when the entry applies
   * </p>
   */
  @Override
  public void setPersistTemporalUnit(ChronoUnit persistTemporalUnit) {
    checkPersistTemporalUnit(persistTemporalUnit);
    this.persistTemporalUnit = persistTemporalUnit;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The holiday settings are asked of the moment's own day, also for the part past midnight
   * </p>
   */
  @Override
  public boolean covers(TimeInWeekWithDayType moment) {
    return isMetBy(moment) && dayIntervals.stream().anyMatch(dayInterval ->
        dayInterval.first() == moment.getDayOfWeek() && dayInterval.second().contains(moment.getMinuteOfDay()));
  }

  /**
   * {@inheritDoc}
   * <p>
   * Pairwise over the day intervals of both, see {@link #covers(TimeInWeekWithDayType)}, so an entry past midnight
   * overlaps on the day after too. For example Monday 21:00 for 8 hours overlaps Tuesday 03:00 to 04:00, and not Monday
   * 03:00 to 04:00. Holidays included overlap holidays only and holidays excluded alike
   * </p>
   */
  @Override
  public boolean overlaps(PricingTimeBandEntry other) {
    if (!publicHolidays.overlaps(other.getPublicHolidays()) || !schoolHolidays.overlaps(other.getSchoolHolidays())) {
      return false;
    }
    var otherDayIntervals = other instanceof PricingTimeBandEntryImpl ?
        ((PricingTimeBandEntryImpl) other).dayIntervals :
        new PricingTimeBandEntryImpl(other.getDays(), other.getPublicHolidays(), other.getSchoolHolidays(),
            other.getStartMinuteOfDay(), other.getDurationMinutes()).dayIntervals;
    return dayIntervals.stream().anyMatch(dayInterval -> otherDayIntervals.stream().anyMatch(otherInterval ->
        otherInterval.first() == dayInterval.first() && otherInterval.second().overlaps(dayInterval.second())));
  }
}
