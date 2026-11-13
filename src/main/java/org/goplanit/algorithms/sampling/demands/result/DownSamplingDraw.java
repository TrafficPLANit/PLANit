package org.goplanit.algorithms.sampling.demands.result;

import java.util.BitSet;

/**
 * What a draw of a down-sampling kept, by row of its down-sampling table, and the chance each zone's sampling units
 * were drawn with
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>draw</b> is one sample; a row
 * of the down-sampling table is one <b>sampling unit</b>, a household with its persons, tours and trips; a
 * zone's <b>chance</b> is the share of its sampling units kept, such as 13 of 130, 0.1.
 * </p>
 * <p>
 * The statistics measure each total against what the draw aimed at: each sampling unit's chance times its value. At
 * households at random every chance is the rate; by zone it is the zone's number kept over its sampling units, which
 * rounds the rate up or down per zone. Handing the chances over with the units kept lets the statistics tell what the
 * draw missed from that rounding.
 * </p>
 * <p>
 * Every down-sampling algorithm returns one from
 * {@link org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingAlgorithmBase#chooseUnitsKept}, so
 * an algorithm added outside this package makes one too.
 * </p>
 *
 * @author markr
 */
public final class DownSamplingDraw {

  /** the sampling units kept, by row of the down-sampling table */
  private final BitSet kept;

  /** the chance of each zone's sampling units, by zone id in the down-sampling table */
  private final double[] chanceByZone;

  /**
   * Constructor
   *
   * @param kept the sampling units kept, by row of the down-sampling table
   * @param chanceByZone the chance of each zone's sampling units, by zone id in the down-sampling table
   */
  public DownSamplingDraw(BitSet kept, double[] chanceByZone) {
    this.kept = kept;
    this.chanceByZone = chanceByZone;
  }

  /**
   * The sampling units kept
   *
   * @return by row of the down-sampling table
   */
  public BitSet getKept() {
    return kept;
  }

  /**
   * The chance a zone's sampling units were drawn with
   *
   * @param zoneIdInTable the zone's id in the down-sampling table, from 0, see
   *     {@link org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable#getZoneIdInTable(int)}
   * @return the chance
   */
  public double getChance(int zoneIdInTable) {
    return chanceByZone[zoneIdInTable];
  }
}
