package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.sampling.BalancedSamplingUnits;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the units of the balanced sampler: which totals are implied by the number of units and the totals before them
 *
 * @author markr
 */
public class BalancedSamplingUnitsTest {

  /** no values for totals kept per stratum */
  private static final double[] NO_STRATUM_VALUES = new double[0];

  /**
   * Totals A of 0 or 1, B between 0 and 1, A + B, 2 for every unit, 0 for every unit, C of 1 for one unit only, and 3A -
   * 2C: A + B, the 2 (twice the count), the 0 and 3A - 2C are implied, A, B and the rare C are not
   */
  @Test
  public void testImpliedTotals() {
    var random = new SplittableRandom(19);
    var units = new BalancedSamplingUnits(7, 0);
    for (int unit = 0; unit < 1000; ++unit) {
      double a = random.nextInt(2);
      double b = random.nextDouble();
      double c = unit == 500 ? 1 : 0;
      units.addUnit(0.1, unit % 10, new double[]{a, b, a + b, 2, 0, c, 3 * a - 2 * c}, NO_STRATUM_VALUES);
    }
    var expected = new BitSet();
    expected.set(2);
    expected.set(3);
    expected.set(4);
    expected.set(6);
    assertEquals(expected, units.collectImpliedTotals());
  }

  /** The order decides which total is implied: with A + B listed before A and B, B is implied and A + B is not */
  @Test
  public void testImpliedTotalFollowsWhatImpliesIt() {
    var random = new SplittableRandom(29);
    var units = new BalancedSamplingUnits(3, 0);
    for (int unit = 0; unit < 1000; ++unit) {
      double a = random.nextInt(2);
      double b = random.nextDouble();
      units.addUnit(0.1, 0, new double[]{a + b, a, b}, NO_STRATUM_VALUES);
    }
    var expected = new BitSet();
    expected.set(2);
    assertEquals(expected, units.collectImpliedTotals());
  }

  /** A total equal to A in all but one of a million units is not implied by A */
  @Test
  public void testTotalDifferingInOneUnitNotImplied() {
    var random = new SplittableRandom(31);
    var units = new BalancedSamplingUnits(2, 0, 1000000);
    for (int unit = 0; unit < 1000000; ++unit) {
      double a = random.nextInt(2);
      units.addUnit(0.1, 0, new double[]{a, unit == 7 ? a + 1 : a}, NO_STRATUM_VALUES);
    }
    assertTrue(units.collectImpliedTotals().isEmpty());
  }
}
