/**
 * Down-sampling a discrete demand to a share of its households: its settings, the down-sampling table of sampling units
 * counted from the demand, the algorithms of each quality and their statistics, on the balanced sampler of
 * {@link org.goplanit.algorithms.sampling}
 * <p>
 * The terms used, besides those of the balanced sampler listed in {@link org.goplanit.algorithms.sampling}:
 * </p>
 * <ul>
 * <li><b>Down-sample rate</b>: the share of households kept, such as 0.1 keeping one in ten. Each sampling unit's
 * chance of being kept is the rate, on average.</li>
 * <li><b>Sampling unit</b>: the unit of the sampler, what is kept or dropped whole: a household with its persons,
 * tours and trips; households whose persons share a tour, together; or a person without a household.</li>
 * <li><b>Quality</b>: how the sampling units are chosen, from households at random to the best of several balanced
 * draws, see {@link org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType}.</li>
 * <li><b>Group of totals</b>: one way of counting the demand, such as person trips by mode, with a total for each of
 * its <b>categories</b>, such as car driven or bus, see
 * {@link org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup}.</li>
 * <li><b>Balance</b>: make the sample's totals of a group meet their targets, at 10% a tenth of the full demand's.</li>
 * <li><b>Checked table</b>: a finer count, such as person trips by mode and hour, measured by the statistics but never
 * balanced, see {@link org.goplanit.algorithms.sampling.demands.enums.DownSamplingCheckedTable}.</li>
 * <li><b>Down-sampling table</b>: the demand as counts, a row per sampling unit and a column per category, which every
 * quality draws from, see {@link org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable}.</li>
 * <li><b>Along the curve</b>: the zones in their order along a Hilbert curve, so zones next to each other in the order
 * lie close together. An <b>area</b> is a run of zones next to each other in that order.</li>
 * <li><b>Zone id in the table</b>: the down-sampling table's number of a zone, its place along the curve from 0, the
 * sampling units without a zone taking one more, the last. These zones are the balanced sampler's strata: each keeps
 * its sampling units times the rate, rounded down or up.</li>
 * <li><b>Random spread</b>: how far a total would typically be off in a draw of households at random at the rate.</li>
 * <li><b>Error</b>: how far a group's totals are off together, see
 * {@link org.goplanit.algorithms.sampling.demands.result.DownSamplingTotalsSummary}. The <b>score</b> is the checked
 * tables' error over a random draw's, lower being better, and picks the best of several draws.</li>
 * </ul>
 */
package org.goplanit.algorithms.sampling.demands;
