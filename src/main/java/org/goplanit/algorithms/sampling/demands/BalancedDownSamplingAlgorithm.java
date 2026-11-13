package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;

import java.util.List;

/**
 * Down-sampling at the balanced quality: as by zone, and keeping the full demand's totals of each group of the
 * settings, times each zone's share kept, as closely as whole sampling units allow, see
 * {@link DiscreteDemandsDownSamplingSettings#getBalanceOrder()}
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>sampling unit</b> is a
 * household with its persons, tours and trips, kept or dropped whole; a <b>group</b> of totals is one way of counting
 * the demand, such as person trips by mode, with a total for each <b>category</b>, such as car driven; a zone's
 * <b>share kept</b> is its sampling units kept over its sampling units, about the down-sample rate.
 * </p>
 * <p>
 * A sample of households drawn at random, even zone by zone, matches the full demand's car trips, kilometres and
 * persons only give or take the random spread, and with a hundred totals some are always far off. The balanced draw,
 * the cube method of {@link org.goplanit.algorithms.sampling.BalancedSampler}, keeps every household's chance at the
 * rate while only samples whose totals match can come out. Where not every total can be met, the least important is
 * given up first, the groups in the order of the settings and each group's categories in their order.
 * </p>
 * <p>
 * Persons per zone, the one total per zone, are given up before every total over all zones, wherever they stand in
 * the order. A zone's persons times the rate is a whole number in only about one zone in ten, so nearly every zone
 * ends with households no step can move while its persons are kept. Given up first, those households are settled
 * while every total over all zones is still kept; given up last, every total over all zones would take the rounding
 * of all of them, which on 2.6 million households put the totals ten times as far off. Each zone still keeps its
 * number of households exactly, and its persons to within about one person on average.
 * </p>
 *
 * @author markr
 */
public class BalancedDownSamplingAlgorithm extends ByZoneDownSamplingAlgorithm {

  /**
   * Constructor
   *
   * @param settings to use, owned by the algorithm
   */
  protected BalancedDownSamplingAlgorithm(DiscreteDemandsDownSamplingSettings settings) {
    super(settings);
  }

  /**
   * The groups of totals the draw keeps, the most important first
   *
   * @return the groups of the settings, in their order
   */
  @Override
  protected List<DownSamplingBalanceGroup> getGroupsBalanced() {
    return getSettings().getBalanceOrder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingAlgorithmType getType() {
    return DiscreteDemandsDownSamplingAlgorithmType.BALANCED;
  }
}
