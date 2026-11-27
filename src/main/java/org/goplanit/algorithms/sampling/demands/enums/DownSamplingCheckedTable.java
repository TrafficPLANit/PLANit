package org.goplanit.algorithms.sampling.demands.enums;

/**
 * The tables a down-sampling checks but never balances, each a cross of two or three ways of counting the demand
 * <p>
 * Balancing every cross, such as trips by mode in each hour, would take hundreds of totals and slow the draw several
 * times over. Checking them instead shows whether matching the groups of totals, see {@link DownSamplingBalanceGroup},
 * keeps what they do not hold, or pulls it off: a sample that matches trips by mode and trips by departure period
 * could still put too many car trips in the morning peak.
 * </p>
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: to <b>balance</b> a total is to
 * make the sample's total come out at the full demand's times the rate, at 10% a tenth; a checked table is only
 * measured, cell by cell, by
 * {@link org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingStatistics}.
 * </p>
 *
 * @author markr
 */
public enum DownSamplingCheckedTable {

  /** person trips by the mode of trips by mode, driven and ridden apart, and the hour of departure on the clock */
  PERSON_TRIPS_BY_MODE_AND_HOUR,

  /** person trips by the destination area of their origin zone, that of their destination zone, and mode group */
  PERSON_TRIPS_BY_AREAS_AND_MODE_GROUP,

  /** person trips by the mode of trips by mode and the distance band */
  PERSON_TRIPS_BY_MODE_AND_DISTANCE_BAND,

  /** top-level tours by purpose and the mode of their first trip */
  TOURS_BY_PURPOSE_AND_MODE,

  /** persons by age band and sex */
  PERSONS_BY_AGE_BAND_AND_SEX
}
