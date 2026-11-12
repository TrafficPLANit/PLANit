package org.goplanit.algorithms.sampling.demands.result;

import java.util.BitSet;
import java.util.function.Supplier;

/**
 * What a draw from a {@link org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable} kept: the
 * households and the persons without a household kept, by their ids in the demand the down-sampling table was counted
 * from, and the statistics of the draw
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>draw</b> is one sample of the
 * down-sampling table, from one seed.
 * </p>
 * <p>
 * A draw names what it keeps rather than removing the rest, so one down-sampling table can be drawn from many times,
 * at other seeds or qualities, and the demand stays as it was until a caller removes what is not named. Internal ids
 * are contiguous, so a bit set names them in a bit each.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingResult {

  /** the ids of the households kept */
  private final BitSet householdIdsKept;

  /** the ids of the persons without a household kept */
  private final BitSet personIdsWithoutHouseholdKept;

  /** computes the statistics of the draw, when first asked for */
  private final Supplier<DiscreteDemandsDownSamplingStatistics> computeStatistics;

  /** the statistics of the draw, null until first asked for */
  private DiscreteDemandsDownSamplingStatistics statistics;

  /**
   * Constructor
   *
   * @param householdIdsKept the ids of the households kept, in the demand, copied
   * @param personIdsWithoutHouseholdKept the ids of the persons without a household kept, in the demand, copied
   * @param computeStatistics computes the statistics of the draw, called when they are first asked for
   */
  public DiscreteDemandsDownSamplingResult(BitSet householdIdsKept, BitSet personIdsWithoutHouseholdKept,
      Supplier<DiscreteDemandsDownSamplingStatistics> computeStatistics) {
    this.householdIdsKept = (BitSet) householdIdsKept.clone();
    this.personIdsWithoutHouseholdKept = (BitSet) personIdsWithoutHouseholdKept.clone();
    this.computeStatistics = computeStatistics;
  }

  /**
   * The households kept
   *
   * @return their ids in the demand, a copy
   */
  public BitSet getHouseholdIdsKept() {
    return (BitSet) householdIdsKept.clone();
  }

  /**
   * The persons without a household kept
   *
   * @return their ids in the demand, a copy
   */
  public BitSet getPersonIdsWithoutHouseholdKept() {
    return (BitSet) personIdsWithoutHouseholdKept.clone();
  }

  /**
   * The statistics of the draw, the same as down-sampling the demand in place would give. They take a pass over the
   * down-sampling table, so they are computed when first asked for: a caller drawing many times, such as for best of
   * several, only pays for those it reads
   *
   * @return the statistics
   */
  public DiscreteDemandsDownSamplingStatistics getStatistics() {
    if (statistics == null) {
      statistics = computeStatistics.get();
    }
    return statistics;
  }
}
