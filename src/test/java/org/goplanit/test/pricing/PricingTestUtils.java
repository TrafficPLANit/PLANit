package org.goplanit.test.pricing;

import org.goplanit.pricing.*;
import org.goplanit.pricing.FromToGatesPriceRuleImpl;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.pricing.FromToGatesPriceRule;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.goplanit.utils.time.TimeInWeekWithDayType;

/**
 * Shorthands the pricing tests share, to make time band entries and moments in one line
 * <p>
 * Pricing tests give entries in minutes from midnight and moments by the hour, as the scenarios do: "weekdays 07:00 to
 * 10:00" is {@code entry(WEEKDAYS, 420, 180)}, "07:00 on a Tuesday" {@code at(DayOfWeek.TUESDAY, 7)}.
 * </p>
 *
 * @author markr
 */
public final class PricingTestUtils {

  /** No instances */
  private PricingTestUtils() {
  }

  /** Monday to Friday */
  public static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

  /**
   * A pricing of a base price rule with a start only
   *
   * @param start of the base price rule
   * @return the pricing
   */
  public static SchemePricing<StartWithPerKmPriceRule> startOf(double start) {
    return new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(start, null, List.of()), List.of(),
        List.of(), List.of(), List.of());
  }

  /**
   * A pricing of a base price rule only
   *
   * @param basePriceRule of the pricing
   * @return the pricing
   */
  public static SchemePricing<StartWithPerKmPriceRule> baseOf(StartWithPerKmPriceRule basePriceRule) {
    return new StartWithPerKmSchemePricingImpl(basePriceRule, List.of(), List.of(), List.of(), List.of());
  }

  /**
   * A pricing of an entry-exit scheme with a base price rule of gate pairs only
   *
   * @param gatePairs of the base price rule, may be none
   * @return the pricing
   */
  public static SchemePricing<FromToGatesPriceRule> gatePairsOf(GateFromToAmount... gatePairs) {
    return new FromToGatesSchemePricingImpl(new FromToGatesPriceRuleImpl(List.of(gatePairs)), List.of(), List.of(),
        List.of(), List.of());
  }

  /**
   * Distance bands counted per day: the first 20 km free, factor 0, and a quarter less from 200 km, factor 0.75 ("Free
   * first kilometres and a cheaper long distance")
   *
   * @return the distance bands
   */
  public static List<PricingDistanceBand> distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km() {
    return List.of(new PricingDistanceBandImpl(PricingPeriod.DAY, null, 20.0, 0),
        new PricingDistanceBandImpl(PricingPeriod.DAY, 200.0, null, 0.75));
  }

  /**
   * A pricing with time bands only
   *
   * @param bands of the pricing
   * @return the pricing
   */
  @SafeVarargs
  public static SchemePricing<StartWithPerKmPriceRule> bandsOf(PricingTimeBand<StartWithPerKmPriceRule>... bands) {
    return new StartWithPerKmSchemePricingImpl(null, List.of(), List.of(bands), List.of(), List.of());
  }

  /**
   * An entry in minutes, holidays left out
   *
   * @param days it starts on, empty for every day
   * @param fromMinute of the day it starts at
   * @param minutes it lasts
   * @return the entry
   */
  public static PricingTimeBandEntryImpl entry(Set<DayOfWeek> days, int fromMinute, int minutes) {
    return entry(days, null, fromMinute, minutes);
  }

  /**
   * An entry in minutes, school holidays left out
   *
   * @param days it starts on, empty for every day
   * @param publicHolidays whether public holidays count, null when left out
   * @param fromMinute of the day it starts at
   * @param minutes it lasts
   * @return the entry
   */
  public static PricingTimeBandEntryImpl entry(
      Set<DayOfWeek> days,
      HolidayInclusion publicHolidays,
      int fromMinute,
      int minutes) {
    return new PricingTimeBandEntryImpl(days, publicHolidays, null, fromMinute, minutes);
  }

  /**
   * A moment on the hour that is no holiday
   *
   * @param day of the week
   * @param hour of the day
   * @return the moment
   */
  public static TimeInWeekWithDayType at(DayOfWeek day, int hour) {
    return at(day, hour, 0);
  }

  /**
   * A moment that is no holiday
   *
   * @param day of the week
   * @param hour of the day
   * @param minute of the hour
   * @return the moment
   */
  public static TimeInWeekWithDayType at(DayOfWeek day, int hour, int minute) {
    return TimeInWeekWithDayType.of(day, hour * 60 + minute, false, false);
  }
}
