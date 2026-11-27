package org.goplanit.algorithms.sampling.demands.enums;

/**
 * The quality a discrete demand is down-sampled at, from the quickest to the closest match
 * <p>
 * Every quality gives each household, or group of households kept together, the same chance of being kept, the
 * down-sample rate, so the sample stands for the full demand scaled down by it. They differ in how closely the sample
 * keeps the full demand's shape, and in time: from seconds for households at random to minutes for the best of several
 * balanced draws. A balanced draw is one whose totals, such as car driven trips, come out at a tenth of the full
 * demand's at 10%; all terms are listed in {@link org.goplanit.algorithms.sampling.demands}.
 * </p>
 *
 * @author markr
 */
public enum DiscreteDemandsDownSamplingAlgorithmType {

  /** each household kept on its own with the rate's chance; a baseline to compare with */
  HOUSEHOLDS_AT_RANDOM,

  /**
   * each zone keeps its households times the rate, rounded along the zones' spatial order, the households drawn at
   * random within the zone
   */
  BY_ZONE,

  /** as by zone, and balanced on the groups of totals of the settings, in their order */
  BALANCED,

  /**
   * balanced several times, with seeds made from the seed and the draw number, keeping the draw that matches the full
   * demand best
   */
  BALANCED_BEST_OF_SEVERAL
}
