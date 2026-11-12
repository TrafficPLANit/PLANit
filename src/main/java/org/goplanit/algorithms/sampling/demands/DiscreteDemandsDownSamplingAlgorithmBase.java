package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingResult;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingStatistics;
import org.goplanit.algorithms.sampling.demands.result.DownSamplingDraw;
import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.mode.Mode;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;

import java.util.BitSet;
import java.util.Map;
import java.util.logging.Logger;

/**
 * What the down-sampling algorithms share: counting the demand into a {@link DiscreteDemandsDownSamplingTable},
 * removing what is not kept from the demand, or naming what a draw keeps without changing it. Each quality, see
 * {@link DiscreteDemandsDownSamplingAlgorithmType}, only chooses the sampling units kept, see
 * {@link #chooseUnitsKept(DiscreteDemandsDownSamplingTable)}
 * <p>
 * A sampling unit is kept or removed whole: its households with their persons, tours and trips, and its persons
 * without a household. So a household is never split from its persons, nor a tour from those taking part in it. All
 * terms are listed in {@link org.goplanit.algorithms.sampling.demands}.
 * </p>
 *
 * @author markr
 */
public abstract class DiscreteDemandsDownSamplingAlgorithmBase implements DiscreteDemandsDownSamplingAlgorithm {

  /** the logger */
  private static final Logger LOGGER =
      Logger.getLogger(DiscreteDemandsDownSamplingAlgorithmBase.class.getCanonicalName());

  /**
   * Remove from a demand the sampling units not kept: each of their households with its persons and everything they
   * do, and each of their persons without a household
   *
   * @param demands the down-sampling table was counted from
   * @param table the demand was counted into
   * @param kept the sampling units kept, by row of the down-sampling table
   */
  private static void removeUnitsNotKept(
      DiscreteDemands demands, DiscreteDemandsDownSamplingTable table, BitSet kept) {
    var personsByHousehold = demands.getPersons().createGroupByIndex(Person::getHousehold);
    var modifier = demands.getDiscreteDemandsModifier();
    for (int unit = kept.nextClearBit(0); unit < table.size(); unit = kept.nextClearBit(unit + 1)) {
      for (long id : table.getHouseholdIds(unit)) {
        modifier.removeHousehold(demands.getHouseholds().get(id), personsByHousehold);
      }
      for (long id : table.getPersonIdsWithoutHousehold(unit)) {
        modifier.removePerson(demands.getPersons().get(id));
      }
    }
  }

  /** the settings, a copy of those given */
  private final DiscreteDemandsDownSamplingSettings settings;

  /**
   * Constructor
   *
   * @param settings to use, owned by the algorithm
   */
  protected DiscreteDemandsDownSamplingAlgorithmBase(DiscreteDemandsDownSamplingSettings settings) {
    this.settings = settings;
  }

  /**
   * The settings of the algorithm
   *
   * @return the settings
   */
  protected DiscreteDemandsDownSamplingSettings getSettings() {
    return settings;
  }

  /**
   * Choose the sampling units kept, each with the same chance on average, the down-sample rate
   *
   * @param table to choose from, not changed
   * @return the sampling units kept, by row of the down-sampling table, and the chance each zone's were drawn with
   */
  protected abstract DownSamplingDraw chooseUnitsKept(DiscreteDemandsDownSamplingTable table);

  /**
   * The statistics of a draw, logged, and written to the settings' statistics file when there is one
   *
   * @param table drawn from
   * @param draw made
   * @return the statistics
   */
  protected DiscreteDemandsDownSamplingStatistics reportStatistics(
      DiscreteDemandsDownSamplingTable table, DownSamplingDraw draw) {
    var statistics = new DiscreteDemandsDownSamplingStatistics(table, draw, settings.getDownSampleRate());
    statistics.log();
    if (settings.getStatisticsFile() != null) {
      statistics.writeCsv(settings.getStatisticsFile());
    }
    return statistics;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingTable createDownSamplingTable(
      DiscreteDemands demands, Zoning zoning, Map<Mode, OdSkimMatrix> distances) {
    return new DiscreteDemandsDownSamplingTable(settings, zoning, distances, demands);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingResult drawFrom(DiscreteDemandsDownSamplingTable table) {
    var draw = chooseUnitsKept(table);
    var householdIdsKept = new BitSet();
    var personIdsWithoutHouseholdKept = new BitSet();
    for (int unit = draw.getKept().nextSetBit(0); unit >= 0; unit = draw.getKept().nextSetBit(unit + 1)) {
      for (long id : table.getHouseholdIds(unit)) {
        householdIdsKept.set(Math.toIntExact(id));
      }
      for (long id : table.getPersonIdsWithoutHousehold(unit)) {
        personIdsWithoutHouseholdKept.set(Math.toIntExact(id));
      }
    }
    return new DiscreteDemandsDownSamplingResult(householdIdsKept, personIdsWithoutHouseholdKept,
        () -> new DiscreteDemandsDownSamplingStatistics(table, draw, settings.getDownSampleRate()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingStatistics downSample(
      DiscreteDemands demands, Zoning zoning, Map<Mode, OdSkimMatrix> distances, boolean recreateManagedIds) {
    var table = createDownSamplingTable(demands, zoning, distances);
    var draw = chooseUnitsKept(table);
    var statistics = reportStatistics(table, draw);
    removeUnitsNotKept(demands, table, draw.getKept());
    if (recreateManagedIds) {
      demands.getDiscreteDemandsModifier().recreateManagedEntitiesIds();
    }
    LOGGER.info(String.format(
        "Down-sampled at %s to a rate of %s: %d of %d sampling units kept, leaving %d households and %d persons",
        getType(), settings.getDownSampleRate(), draw.getKept().cardinality(), table.size(),
        demands.getHouseholds().size(), demands.getPersons().size()));
    return statistics;
  }
}
