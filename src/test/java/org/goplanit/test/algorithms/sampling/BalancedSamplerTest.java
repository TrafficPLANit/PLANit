package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.sampling.BalancedSampler;
import org.goplanit.algorithms.sampling.BalancedSamplingUnits;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the balanced sampler: each unit kept with its chance, each stratum keeping its number of units, and the totals
 * kept as close as the units allow, giving up the least important first
 *
 * @author markr
 */
public class BalancedSamplerTest {

  /** no values for totals kept per stratum */
  private static final double[] NO_STRATUM_VALUES = new double[0];

  /**
   * Count the units kept whose value at the given position is 1
   *
   * @param kept the units kept
   * @param values of each unit
   * @param position of the value
   * @return the number of units kept with that value 1
   */
  private static int countKeptWithValueOne(BitSet kept, double[][] values, int position) {
    int count = 0;
    for (int unit = kept.nextSetBit(0); unit >= 0; unit = kept.nextSetBit(unit + 1)) {
      if (values[unit][position] == 1.0) {
        ++count;
      }
    }
    return count;
  }

  /**
   * Units of one stratum with the given chance and values
   *
   * @param chance of each unit
   * @param values of each unit for the totals over all units
   * @return the units
   */
  private static BalancedSamplingUnits createOneStratum(double chance, double[][] values) {
    var units = new BalancedSamplingUnits(values[0].length, 0);
    for (double[] unitValues : values) {
      units.addUnit(chance, 0, unitValues, NO_STRATUM_VALUES);
    }
    return units;
  }

  /**
   * 100 units, a count A of 1 for 30 of them spread through the order, three in every ten, and values B and C drawn
   * between 0 and 1, in the order given. With A and one value only, A would be kept exactly in either order: the units
   * left undecided with A of 1 always have chances adding up to a whole number, so the landing never needs to give A
   * up. A second value makes it give up something. The units with A are spread, as with all of them first the steps
   * would settle them while the others pass, before the landing
   *
   * @param countFirst when true the order is A, B, C, otherwise B, C, A
   * @return the values of each unit in the order asked for
   */
  private static double[][] createCountAndValues(boolean countFirst) {
    var random = new SplittableRandom(7);
    double[][] values = new double[100][];
    for (int unit = 0; unit < values.length; ++unit) {
      double countA = unit % 10 < 3 ? 1 : 0;
      double valueB = random.nextDouble();
      double valueC = random.nextDouble();
      values[unit] = countFirst ? new double[]{countA, valueB, valueC} : new double[]{valueB, valueC, countA};
    }
    return values;
  }

  /**
   * Over 2,000 draws with different seeds, each of 1,000 units with chance 0.1 is kept in between 130 and 270 of them,
   * five times the spread chance alone gives, and the units are kept in 199 to 201 draws on average
   */
  @Test
  public void testEachUnitKeptWithItsChance() {
    var random = new SplittableRandom(3);
    double[][] values = new double[1000][];
    for (int unit = 0; unit < values.length; ++unit) {
      values[unit] = new double[]{random.nextDouble()};
    }
    var units = createOneStratum(0.1, values);
    int[] timesKept = new int[values.length];
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 2000; ++seed) {
      var kept = sampler.draw(units, seed);
      kept.stream().forEach(unit -> ++timesKept[unit]);
    }
    double sum = 0;
    for (int count : timesKept) {
      assertTrue(count >= 130 && count <= 270);
      sum += count;
    }
    double mean = sum / timesKept.length;
    assertTrue(mean >= 199 && mean <= 201);
  }

  /** A unit with chance 1 is kept in every draw, and one with chance 0 in none */
  @Test
  public void testChanceOneAlwaysKeptAndChanceZeroNever() {
    var units = new BalancedSamplingUnits(1, 0);
    units.addUnit(1.0, 0, new double[]{1}, NO_STRATUM_VALUES);
    units.addUnit(0.0, 0, new double[]{1}, NO_STRATUM_VALUES);
    for (int unit = 0; unit < 18; ++unit) {
      units.addUnit(0.5, 0, new double[]{unit % 3}, NO_STRATUM_VALUES);
    }
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 100; ++seed) {
      var kept = sampler.draw(units, seed);
      assertTrue(kept.get(0));
      assertFalse(kept.get(1));
    }
  }

  /** A stratum of 30 units with chance 0.1 and one of 20 units with chance 0.15 keep exactly 3 units each, every draw */
  @Test
  public void testStrataKeepTheirNumberOfUnits() {
    var random = new SplittableRandom(5);
    var units = new BalancedSamplingUnits(1, 0);
    for (int unit = 0; unit < 50; ++unit) {
      units.addUnit(unit < 30 ? 0.1 : 0.15, unit < 30 ? 0 : 1, new double[]{random.nextDouble()}, NO_STRATUM_VALUES);
    }
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 100; ++seed) {
      var kept = sampler.draw(units, seed);
      assertEquals(3, kept.get(0, 30).cardinality());
      assertEquals(3, kept.get(30, 50).cardinality());
    }
  }

  /** A stratum whose chances add up to 2.5 is refused */
  @Test
  public void testStratumSumNotWholeRefused() {
    var units = new BalancedSamplingUnits(1, 0);
    for (int unit = 0; unit < 5; ++unit) {
      units.addUnit(0.5, 0, new double[]{1}, NO_STRATUM_VALUES);
    }
    assertThrows(IllegalArgumentException.class, () -> new BalancedSampler(1).draw(units, 42));
  }

  /** 100 units with chance 0.5, half of them with value 1, keep 50 units with 25 of value 1, every draw */
  @Test
  public void testTotalThatCanBeMet() {
    double[][] values = new double[100][];
    for (int unit = 0; unit < values.length; ++unit) {
      values[unit] = new double[]{unit < 50 ? 1 : 0};
    }
    var units = createOneStratum(0.5, values);
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 100; ++seed) {
      var kept = sampler.draw(units, seed);
      assertEquals(50, kept.cardinality());
      assertEquals(25, countKeptWithValueOne(kept, values, 0));
    }
  }

  /**
   * With a count A of 1 for 30 of 100 units listed first and values B and C between 0 and 1 after it, every draw
   * keeps exactly 15 units with A, and B's total differs from its target by less than twice the largest value of B
   */
  @Test
  public void testLeastImportantTotalGivenUpFirst() {
    double[][] values = createCountAndValues(true);
    var units = createOneStratum(0.5, values);
    double targetB = 0;
    double largestB = 0;
    for (double[] unitValues : values) {
      targetB += 0.5 * unitValues[1];
      largestB = Math.max(largestB, unitValues[1]);
    }
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 100; ++seed) {
      var kept = sampler.draw(units, seed);
      assertEquals(15, countKeptWithValueOne(kept, values, 0));
      double totalB = kept.stream().mapToDouble(unit -> values[unit][1]).sum();
      assertTrue(Math.abs(totalB - targetB) < 2 * largestB);
    }
  }

  /** With B and C listed first and A last, some of 200 draws keep a number of units with A other than 15 */
  @Test
  public void testOrderReversed() {
    double[][] values = createCountAndValues(false);
    var units = createOneStratum(0.5, values);
    var sampler = new BalancedSampler(1);
    boolean otherThanFifteen = false;
    for (int seed = 0; seed < 200 && !otherThanFifteen; ++seed) {
      otherThanFifteen = countKeptWithValueOne(sampler.draw(units, seed), values, 2) != 15;
    }
    assertTrue(otherThanFifteen);
  }

  /**
   * Households in 20 zones, each a stratum, with their persons as a total per stratum and their trips as a total over
   * all: each zone keeps its number of households exactly, and its persons to within the largest household
   */
  @Test
  public void testPersonsPerStratumAsCloseAsTheHouseholdsAllow() {
    final int largestHousehold = 6;
    var random = new SplittableRandom(11);
    var units = new BalancedSamplingUnits(1, 1);
    int[] zoneStart = new int[21];
    double[] personsTarget = new double[20];
    int[] persons = new int[20 * 120];
    int unit = 0;
    for (int zone = 0; zone < 20; ++zone) {
      zoneStart[zone] = unit;
      int households = 40 + 10 * (zone % 9);
      for (int household = 0; household < households; ++household, ++unit) {
        persons[unit] = 1 + random.nextInt(largestHousehold);
        personsTarget[zone] += 0.1 * persons[unit];
        units.addUnit(0.1, zone, new double[]{random.nextInt(11)}, new double[]{persons[unit]});
      }
    }
    zoneStart[20] = unit;
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 50; ++seed) {
      var kept = sampler.draw(units, seed);
      for (int zone = 0; zone < 20; ++zone) {
        final int first = zoneStart[zone];
        var keptInZone = kept.get(first, zoneStart[zone + 1]);
        assertEquals((zoneStart[zone + 1] - first) / 10, keptInZone.cardinality());
        double personsKept = keptInZone.stream().mapToDouble(offset -> persons[first + offset]).sum();
        assertTrue(Math.abs(personsKept - personsTarget[zone]) <= largestHousehold);
      }
    }
  }

  /**
   * Households in 20 zones of 60, with their persons as a total per stratum and a count A over all, 1 for three
   * households in every ten, 36 in all at a chance of 0.1. The zones' persons targets are rarely whole, so most zones
   * end with households only the landing can settle; as it gives up the persons per stratum before A, every draw keeps
   * exactly 36 households with A, and each zone its number of households
   */
  @Test
  public void testTotalOverAllUnitsKeptBeforePersonsPerStratum() {
    var random = new SplittableRandom(17);
    var units = new BalancedSamplingUnits(1, 1);
    double[][] values = new double[20 * 60][];
    for (int unit = 0; unit < values.length; ++unit) {
      values[unit] = new double[]{unit % 10 < 3 ? 1 : 0};
      units.addUnit(0.1, unit / 60, values[unit], new double[]{1 + random.nextInt(6)});
    }
    var sampler = new BalancedSampler(1);
    for (int seed = 0; seed < 50; ++seed) {
      var kept = sampler.draw(units, seed);
      assertEquals(36, countKeptWithValueOne(kept, values, 0));
      for (int zone = 0; zone < 20; ++zone) {
        assertEquals(6, kept.get(zone * 60, zone * 60 + 60).cardinality());
      }
    }
  }

  /**
   * 10,000 units in 200 strata of 50, with a total over all and persons per stratum, so what the strata leave is
   * settled in several runs of strata side by side and then together: over 400 draws on four threads each unit is kept
   * in between 10 and 70 of them, five times the spread chance alone gives around the 40 of its chance of 0.1
   */
  @Test
  public void testEachUnitKeptWithItsChanceAcrossManyStrata() {
    var random = new SplittableRandom(23);
    var units = new BalancedSamplingUnits(1, 1);
    for (int unit = 0; unit < 10000; ++unit) {
      units.addUnit(0.1, unit / 50, new double[]{random.nextDouble()}, new double[]{1 + random.nextInt(6)});
    }
    int[] timesKept = new int[units.size()];
    var sampler = new BalancedSampler(4);
    for (int seed = 0; seed < 400; ++seed) {
      sampler.draw(units, seed).stream().forEach(unit -> ++timesKept[unit]);
    }
    for (int count : timesKept) {
      assertTrue(count >= 10 && count <= 70);
    }
  }

  /**
   * The same units and seed give the same sample twice, and on one thread as on eight; with 200 zones what they leave
   * is settled in several runs of zones
   */
  @Test
  public void testSameSampleOnOneAndEightThreads() {
    var random = new SplittableRandom(13);
    var units = new BalancedSamplingUnits(5, 1);
    for (int zone = 0; zone < 200; ++zone) {
      int households = 60 + 10 * (zone % 9);
      for (int household = 0; household < households; ++household) {
        units.addUnit(0.1, zone, random.doubles(5, 0, 10).toArray(), new double[]{1 + random.nextInt(6)});
      }
    }
    var onOneThread = new BalancedSampler(1).draw(units, 42);
    assertEquals(onOneThread, new BalancedSampler(1).draw(units, 42));
    assertEquals(onOneThread, new BalancedSampler(8).draw(units, 42));
  }
}
