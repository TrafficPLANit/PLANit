package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingResult;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingStatistics;
import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.utils.mode.Mode;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;

import java.util.Map;

/**
 * An algorithm down-sampling a discrete demand to a share of its households, each kept whole with its persons, tours
 * and trips, and with the same chance as every other. Create one with
 * {@link DiscreteDemandsDownSamplingAlgorithmFactory}.
 * <p>
 * Terms used throughout, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>sampling unit</b> is what
 * is kept or dropped whole, a household with its persons, tours and trips; the <b>down-sample rate</b> is the share of
 * households kept, and each sampling unit's <b>chance</b> of being kept; a <b>quality</b> is how the sampling units are
 * chosen.
 * </p>
 * <p>
 * Agent-based models such as MATSim often run on a sample of the demand, with the network's capacities scaled by the
 * same share, to save time. A sample drawn at random spreads over the zones and matches the full demand's trips only
 * give or take the random spread, which shows in the link volumes. The algorithms of the higher qualities keep each
 * zone's share of households and, at the balanced ones, the full demand's trips, kilometres and persons as closely as
 * whole households allow, see {@link DiscreteDemandsDownSamplingAlgorithmType}.
 * </p>
 * <p>
 * A demand is down-sampled in place by {@link #downSample(DiscreteDemands, Zoning, Map, boolean)}. To draw from it
 * more than once, at other seeds or qualities, count it once into a {@link DiscreteDemandsDownSamplingTable}, see
 * {@link #createDownSamplingTable(DiscreteDemands, Zoning, Map)}, and draw from the down-sampling table by
 * {@link #drawFrom(DiscreteDemandsDownSamplingTable)}, which names what is kept without changing the demand.
 * </p>
 *
 * @author markr
 */
public interface DiscreteDemandsDownSamplingAlgorithm {

  /**
   * The type of this algorithm
   *
   * @return its type
   */
  public abstract DiscreteDemandsDownSamplingAlgorithmType getType();

  /**
   * Down-sample a demand: draw its sampling units and remove the households and persons without a household not drawn,
   * with everything they do. The demand is checked in full before anything is removed
   *
   * @param demands to down-sample, changed in place
   * @param zoning whose OD zones the demand's zones are
   * @param distances complete distance skim in km of each mode, each trip's distance coming from its mode's
   * @param recreateManagedIds when true, the households, persons, tours and trips left are numbered from 0 again in the
   *                           order they had, so their ids are contiguous; their XML ids stay as they were either way
   * @return the statistics of the sample, also logged, and written to the settings' statistics file when there is one
   * @throws IllegalArgumentException when the demand is refused, see
   *                                  {@link DiscreteDemandsDownSamplingTable#DiscreteDemandsDownSamplingTable}
   */
  public abstract DiscreteDemandsDownSamplingStatistics downSample(
      DiscreteDemands demands, Zoning zoning, Map<Mode, OdSkimMatrix> distances, boolean recreateManagedIds);

  /**
   * A table counted from a demand, for {@link #drawFrom(DiscreteDemandsDownSamplingTable)} as often as needed
   *
   * @param demands to count, not changed
   * @param zoning whose OD zones the demand's zones are
   * @param distances complete distance skim in km of each mode, each trip's distance coming from its mode's
   * @return the down-sampling table
   * @throws IllegalArgumentException when the demand is refused, see
   *                                  {@link DiscreteDemandsDownSamplingTable#DiscreteDemandsDownSamplingTable}
   */
  public abstract DiscreteDemandsDownSamplingTable createDownSamplingTable(
      DiscreteDemands demands, Zoning zoning, Map<Mode, OdSkimMatrix> distances);

  /**
   * Draw the sampling units kept from a down-sampling table, without changing it, so the same down-sampling table can
   * be drawn from again
   *
   * @param table to draw from
   * @return the households and persons without a household kept, by their ids in the demand, and the statistics,
   *         computed when first asked for; the caller logs or writes them as it needs
   */
  public abstract DiscreteDemandsDownSamplingResult drawFrom(DiscreteDemandsDownSamplingTable table);
}
