package org.goplanit.algorithms.sampling.demands.enums;

/**
 * What the statistics of a down-sampling measure zone by zone
 * <p>
 * The groups of totals hold the region as a whole; only the households and persons are kept per zone. These measures
 * show how far each zone is from what it should hold, next to the spread a random draw of households within the zone
 * would give, so the effect of balancing the region rather than each zone is measured, not assumed.
 * </p>
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>group of totals</b> is one way
 * of counting the demand, such as person trips by mode, see {@link DownSamplingBalanceGroup}; to <b>balance</b> it is to
 * make the sample's totals come out at the full demand's times the rate.
 * </p>
 *
 * @author markr
 */
public enum DownSamplingZoneMeasure {

  /**
   * the households of the sampling units placed in the zone, a sampling unit being placed in its first household's
   * zone, so two households sharing a tour count in the zone of the first
   */
  HOUSEHOLDS,

  /** the persons of the sampling units placed in the zone, persons without a household included */
  PERSONS,

  /** the car driven trips leaving from the zone, whoever makes them */
  CAR_DRIVEN_TRIPS_BY_ORIGIN_ZONE
}
