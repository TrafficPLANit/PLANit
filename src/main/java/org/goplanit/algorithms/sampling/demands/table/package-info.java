/**
 * The down-sampling table: a discrete demand counted into a row per sampling unit and a column per category, which
 * every down-sampling quality draws from, with the owner-only helpers that gather the demand into its sampling units
 * and keep each person trip's zones. The terms used are listed in {@link org.goplanit.algorithms.sampling.demands}
 */
package org.goplanit.algorithms.sampling.demands.table;
