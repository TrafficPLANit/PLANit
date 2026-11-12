package org.goplanit.algorithms.sampling.demands.enums;

/**
 * A group of totals the balanced down-sampling can match: one total per category, each counted per household by
 * summing over its persons, tours or trips. A value not known counts as a category of its own
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>category</b> is one value of
 * the group's way of counting, such as car driven among the modes; to <b>match</b>, or balance, a group is to make the
 * kept households' total of each category come as close as they allow to the full demand's times the rate, at 10%
 * about a tenth of all car driven trips.
 * </p>
 * <p>
 * A smaller demand stands for the full one only as far as it keeps its shape: the trips by mode, how far and when they
 * go, and the households and persons who make them. Each group is one such side of the demand, matched as a whole
 * over all zones, so the user picks which sides matter and in what order, see
 * {@link org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings#setBalanceOrder}. The households
 * per zone
 * are not a group: every down-sampling but households at random keeps them.
 * </p>
 *
 * @author markr
 */
public enum DownSamplingBalanceGroup {

  /** persons, per zone */
  PERSONS_PER_ZONE,

  /**
   * person trips by each mode in the demand, the trips by a mode its travellers drive split into those the person
   * drives and those they ride in
   */
  PERSON_TRIPS_BY_MODE,

  /**
   * person kilometres by mode group, see {@link DownSamplingModeGroup}: by car as driver, being the vehicle kilometres;
   * by car not as driver; public transport; active; and all other trips
   */
  PERSON_KILOMETRES_BY_MODE_GROUP,

  /** person trips by distance band, between the boundaries of the settings, and by the mode groups above */
  PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP,

  /** person trips by departure period, between the boundaries of the settings */
  PERSON_TRIPS_BY_DEPARTURE_PERIOD,

  /** car driven trips by departure period, the same periods */
  CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD,

  /**
   * tour destinations by area, the areas being runs of zones in their spatial order with equal numbers of tour
   * destinations, as many as the settings say
   */
  TOUR_DESTINATIONS_BY_AREA,

  /** tours by each purpose of a top-level tour in the demand */
  TOURS_BY_PURPOSE,

  /**
   * households by car availability: no car, fewer cars than persons who may drive, at least one car per person who
   * may drive, cars not known
   */
  HOUSEHOLDS_BY_CAR_AVAILABILITY,

  /**
   * households by number of persons known to work full or part time, in the bands of the settings: by default 0, 1, 2,
   * 3 or more
   */
  HOUSEHOLDS_BY_NUMBER_OF_WORKERS,

  /** households by number of cars, in the bands of the settings: by default 0, 1, 2, 3 or more; and not known */
  HOUSEHOLDS_BY_NUMBER_OF_CARS,

  /** households by number of persons, in the bands of the settings: by default 1, 2, 3, 4, 5 or more */
  HOUSEHOLDS_BY_SIZE,

  /**
   * households by income band, as many bands as the settings say, each holding about as many of the full demand's known
   * incomes: by default the fifths; and not known
   */
  HOUSEHOLDS_BY_INCOME_BAND,

  /**
   * persons by age band, in the bands of the settings: by default 0-4, 5-11, 12-17, 18-24, 25-44, 45-64, 65-79, 80 and
   * over; and not known
   */
  PERSONS_BY_AGE_BAND,

  /** persons by employment status: full time, part time, not employed, not known */
  PERSONS_BY_EMPLOYMENT_STATUS,

  /** persons by study status: school, tertiary, not studying, not known */
  PERSONS_BY_STUDY_STATUS,

  /** persons by car licence: yes, no, not known */
  PERSONS_BY_CAR_LICENCE,

  /** persons by sex: female, male, other, not known */
  PERSONS_BY_SEX,

  /**
   * car driven trips by origin area, the areas being runs of zones in their spatial order with about equal numbers of
   * car driven trips leaving them, as many as the settings and the households kept allow, none by default
   */
  CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA
}
