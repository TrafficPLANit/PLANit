/**
 * Algorithms that draw samples: the balanced sampler, which keeps each unit with its chance, each stratum with a fixed
 * number of units and a list of totals as close to their targets as the units allow, by the cube method
 * <p>
 * The terms used, with a 10% sample of households kept zone by zone as the example:
 * </p>
 * <ul>
 * <li><b>Unit</b>: one thing a sample keeps or drops whole, such as a household.</li>
 * <li><b>Chance</b>: how likely a unit is to be kept, from 0 to 1, such as 0.1 for each household of a 10% sample. On
 * average a sample keeps the sum of its units' chances.</li>
 * <li><b>Stratum</b>, plural strata: a group of units that keeps a fixed number of them, such as the households of one
 * zone. A zone of 130 households at 10% keeps exactly 13.</li>
 * <li><b>Total</b>: a sum of one value over the units kept, such as the households' car trips. A total over all units
 * sums over the whole population; a total per stratum sums within each stratum, such as the persons of each zone.</li>
 * <li><b>Target</b>: what a total should come to, each unit's value times its chance summed over all units, such as a
 * tenth of all car trips at 10%.</li>
 * <li><b>Balanced sample</b>: a sample whose totals meet their targets as closely as whole units allow, while each unit
 * is still kept with its chance.</li>
 * <li><b>Draw</b>: one run of the sampler, giving one sample. The same units and seed always give the same draw.</li>
 * <li><b>Cube</b>: the picture the method is named after. Every possible sample is a corner of a cube with an axis per
 * unit, and the chances are a point inside it. {@code CubeFlight} explains it with two households.</li>
 * <li><b>Flight</b>: the method's first phase. It moves the chances a step at a time while every total stays as it is,
 * each step deciding at least one unit.</li>
 * <li><b>Decided</b>: a unit whose chance has reached 0, dropped, or 1, kept. It is undecided while its chance is in
 * between.</li>
 * <li><b>Landing</b>: the method's second phase. When no step is left it gives up the least important total and goes
 * on with the others, until every unit is decided.</li>
 * <li><b>Run of strata</b>: strata next to each other in the order they were added, such as a few neighbouring zones,
 * settled together.</li>
 * <li><b>Implied total</b>: a total that follows from the number of units and the totals before it, such as the last
 * household size class once the number of households and the other classes are kept.</li>
 * </ul>
 */
package org.goplanit.algorithms.sampling;
