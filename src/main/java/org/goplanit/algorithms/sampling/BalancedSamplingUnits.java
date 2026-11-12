package org.goplanit.algorithms.sampling;

import org.goplanit.utils.math.Precision;
import org.goplanit.utils.math.RowEchelonUtils;

import java.util.Arrays;
import java.util.BitSet;

/**
 * The units a {@link BalancedSampler} draws from: per unit its chance of being kept, its stratum, its values for the
 * totals kept over all units and its values for the totals kept per stratum, in the order the units are added. For
 * example, households: each with a chance of 10%, its zone as its stratum, its car trips and car kilometres as values
 * for two totals over all units, and its persons as the value for one total per stratum.
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling}: a <b>unit</b> is one thing the sample keeps
 * or drops whole, such as a household; its <b>chance</b> is how likely it is to be kept, from 0 to 1, such as 0.1 in a
 * 10% sample; its <b>stratum</b> is the group it belongs to, such as its zone, which keeps a fixed number of its units;
 * a <b>total</b> is a sum of one value over the units kept, such as their car trips, either over all units or within
 * each stratum, such as the persons of each zone.
 * </p>
 * <p>
 * A draw over a whole population holds millions of units with a hundred values or more each, so the values sit in flat
 * arrays of primitives, a row of values per unit, grown as units are added, rather than in an object per unit. That way
 * 2.6 million units with 118 values take 2.5 GB, where objects would add a header and a reference per unit and scatter
 * the values over the heap, while the sampler reads a unit's values one after the other. Giving the number of units
 * expected up front saves growing the arrays, which briefly needs the old and the new ones at once.
 * </p>
 * <p>
 * The order the units are added in matters. The sampler settles what each stratum leaves over together with the
 * strata added just before and after it, so a caller that adds the strata in a spatial order, such as zones along a
 * Hilbert curve, has those leftovers settled among neighbours. Within a stratum the units are taken in the order added
 * too.
 * </p>
 *
 * @author markr
 */
public class BalancedSamplingUnits {

  /** units there is room for when no number is expected */
  private static final int DEFAULT_EXPECTED_UNITS = 1024;

  /** the longest array Java can create */
  private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  /** a total is implied when what it adds to the count and the totals before it is below this share of itself */
  private static final double IMPLIED_EPSILON = Precision.EPSILON_9;

  /** number of totals kept over all units, a value each per unit */
  private final int numberOfTotals;

  /** number of totals kept per stratum, a value each per unit */
  private final int numberOfStratumTotals;

  /** the most units the arrays can hold */
  private final int maxUnits;

  /** chance of each unit to be kept */
  private double[] chances;

  /** stratum of each unit */
  private int[] strata;

  /** values for the totals over all units, a row of numberOfTotals per unit */
  private double[] values;

  /** values for the totals per stratum, a row of numberOfStratumTotals per unit */
  private double[] stratumValues;

  /** number of units added */
  private int size;

  /**
   * Make room for at least the given number of units, growing by half when room runs out
   *
   * @param units to make room for
   * @throws IllegalStateException when the units would not fit in an array
   */
  private void makeRoomForUnits(int units) {
    if (units <= chances.length) {
      return;
    }
    if (units > maxUnits) {
      throw new IllegalStateException(String.format(
          "No room for %d units with %d values each, at most %d fit", units,
          Math.max(numberOfTotals, numberOfStratumTotals), maxUnits));
    }
    int length = (int) Math.min(maxUnits, Math.max(units, chances.length + (chances.length >> 1) + 1L));
    chances = Arrays.copyOf(chances, length);
    strata = Arrays.copyOf(strata, length);
    values = Arrays.copyOf(values, length * numberOfTotals);
    stratumValues = Arrays.copyOf(stratumValues, length * numberOfStratumTotals);
  }

  /**
   * Constructor
   *
   * @param numberOfTotals number of totals kept over all units, most important first, such as 2 for car trips and then
   *                       car kilometres
   * @param numberOfStratumTotals number of totals kept per stratum, most important first, such as 1 for the persons of
   *                              each zone
   * @throws IllegalArgumentException when a number is below 0
   */
  public BalancedSamplingUnits(int numberOfTotals, int numberOfStratumTotals) {
    this(numberOfTotals, numberOfStratumTotals, DEFAULT_EXPECTED_UNITS);
  }

  /**
   * Constructor
   *
   * @param numberOfTotals number of totals kept over all units, most important first, such as 2 for car trips and then
   *                       car kilometres
   * @param numberOfStratumTotals number of totals kept per stratum, most important first, such as 1 for the persons of
   *                              each zone
   * @param expectedUnits number of units to make room for up front, such as the households of the demand
   * @throws IllegalArgumentException when a number is below 0
   */
  public BalancedSamplingUnits(int numberOfTotals, int numberOfStratumTotals, int expectedUnits) {
    if (numberOfTotals < 0 || numberOfStratumTotals < 0 || expectedUnits < 0) {
      throw new IllegalArgumentException(String.format(
          "Numbers of totals (%d), totals per stratum (%d) and expected units (%d) may not be below 0", numberOfTotals,
          numberOfStratumTotals, expectedUnits));
    }
    this.numberOfTotals = numberOfTotals;
    this.numberOfStratumTotals = numberOfStratumTotals;
    this.maxUnits = MAX_ARRAY_LENGTH / Math.max(1, Math.max(numberOfTotals, numberOfStratumTotals));
    this.chances = new double[0];
    this.strata = new int[0];
    this.values = new double[0];
    this.stratumValues = new double[0];
    makeRoomForUnits(expectedUnits);
  }

  /**
   * Add a unit after the ones added before
   * <p>
   * For example, a household in zone 12 kept with a chance of 10%, making 4 car trips over 37 km, with 3 persons:
   * {@code addUnit(0.1, 12, new double[] {4, 37}, new double[] {3})}.
   * </p>
   *
   * @param chance of the unit to be kept, from 0 to 1, such as 0.1 for a 10% sample
   * @param stratum of the unit, from 0, such as the position of the household's zone
   * @param unitValues the unit's value for each total over all units, in the order of the totals, such as its car trips
   *                   and car kilometres
   * @param unitStratumValues the unit's value for each total per stratum, in the order of those totals, such as its
   *                          persons
   * @throws IllegalArgumentException when the chance is not from 0 to 1, the stratum below 0, a value not a finite
   *                                  number, or the number of values not the number of totals
   */
  public void addUnit(double chance, int stratum, double[] unitValues, double[] unitStratumValues) {
    if (!(chance >= 0 && chance <= 1)) {
      throw new IllegalArgumentException(String.format("Chance %s of unit %d is not from 0 to 1", chance, size));
    }
    if (stratum < 0) {
      throw new IllegalArgumentException(String.format("Stratum %d of unit %d is below 0", stratum, size));
    }
    if (unitValues.length != numberOfTotals || unitStratumValues.length != numberOfStratumTotals) {
      throw new IllegalArgumentException(String.format(
          "Unit %d has %d values and %d values per stratum, where there are %d totals and %d totals per stratum", size,
          unitValues.length, unitStratumValues.length, numberOfTotals, numberOfStratumTotals));
    }
    if (!Arrays.stream(unitValues).allMatch(Double::isFinite)
        || !Arrays.stream(unitStratumValues).allMatch(Double::isFinite)) {
      throw new IllegalArgumentException(String.format("Unit %d has a value that is not a finite number", size));
    }
    makeRoomForUnits(size + 1);
    chances[size] = chance;
    strata[size] = stratum;
    System.arraycopy(unitValues, 0, values, size * numberOfTotals, numberOfTotals);
    System.arraycopy(unitStratumValues, 0, stratumValues, size * numberOfStratumTotals, numberOfStratumTotals);
    ++size;
  }

  /**
   * Number of units added
   *
   * @return the number of units
   */
  public int size() {
    return size;
  }

  /**
   * Number of totals kept over all units
   *
   * @return the number of totals
   */
  public int getNumberOfTotals() {
    return numberOfTotals;
  }

  /**
   * Number of totals kept per stratum
   *
   * @return the number of totals per stratum
   */
  public int getNumberOfStratumTotals() {
    return numberOfStratumTotals;
  }

  /**
   * Chance of a unit to be kept
   *
   * @param unit index, in the order added from 0
   * @return the chance
   */
  public double getChance(int unit) {
    return chances[unit];
  }

  /**
   * Stratum of a unit
   *
   * @param unit index, in the order added from 0
   * @return the stratum
   */
  public int getStratum(int unit) {
    return strata[unit];
  }

  /**
   * Value of a unit for a total over all units, such as a household's car trips
   *
   * @param unit index, in the order added from 0
   * @param total index, in the order of the totals from 0
   * @return the value
   */
  public double getValue(int unit, int total) {
    return values[unit * numberOfTotals + total];
  }

  /**
   * Value of a unit for a total per stratum, such as a household's persons
   *
   * @param unit index, in the order added from 0
   * @param stratumTotal index, in the order of the totals per stratum from 0
   * @return the value
   */
  public double getStratumValue(int unit, int stratumTotal) {
    return stratumValues[unit * numberOfStratumTotals + stratumTotal];
  }

  /**
   * Collect the totals over all units implied by the number of units and the totals before them: those whose value is,
   * for every unit, the same combination of 1 and the unit's values for the totals before it
   * <p>
   * A total implied this way is kept exactly whenever the count and those totals are, so the sampler leaves it out of
   * its steps, which saves the work it would add to each of them without changing what is kept. Groups of categories
   * are the usual case: households by size class add up to the households, so the last class is implied by the count
   * and the other classes, and persons by age class add up to the persons when those come first. Only totals before it
   * can imply a total, and the landing gives up totals from the end of the list, so whatever implies a total is kept as
   * long as the total itself would be. A total of 0 for every unit is implied by anything.
   * </p>
   * <p>
   * The test covers all units at once, through the sums of the products of every pair of columns, the count's column of
   * ones first and then the totals in their order: a combination of the columns is 0 for every unit exactly when it is
   * 0 for those sums. Each column's sums are scaled by the square root of its own sum of squares, so one tolerance suits
   * values of any size, and Gaussian elimination takes the columns in their order; a column that gets no pivot is
   * implied by the ones before it. A total that differs from such a combination for even one unit in a million is not
   * implied.
   * </p>
   *
   * @return the implied totals, by their index in the order of the totals
   */
  public BitSet collectImpliedTotals() {
    final int columns = numberOfTotals + 1;
    final double[] products = new double[columns * columns];
    final int[] nonZeroColumns = new int[columns];
    final double[] nonZeroValues = new double[columns];
    for (int unit = 0; unit < size; ++unit) {
      final int at = unit * numberOfTotals;
      nonZeroColumns[0] = 0;
      nonZeroValues[0] = 1;
      int count = 1;
      for (int total = 0; total < numberOfTotals; ++total) {
        if (values[at + total] != 0) {
          nonZeroColumns[count] = total + 1;
          nonZeroValues[count++] = values[at + total];
        }
      }
      for (int first = 0; first < count; ++first) {
        final int row = nonZeroColumns[first] * columns;
        final double value = nonZeroValues[first];
        for (int second = first; second < count; ++second) {
          products[row + nonZeroColumns[second]] += value * nonZeroValues[second];
        }
      }
    }

    final double[] scale = new double[columns];
    for (int column = 0; column < columns; ++column) {
      double squares = products[column * columns + column];
      scale[column] = squares > 0 ? 1 / Math.sqrt(squares) : 0;
    }
    for (int row = 0; row < columns; ++row) {
      for (int column = row; column < columns; ++column) {
        double scaled = products[row * columns + column] * scale[row] * scale[column];
        products[row * columns + column] = scaled;
        products[column * columns + row] = scaled;
      }
    }
    final int[] pivotColumns = new int[columns];
    final int rank = RowEchelonUtils.reduceToRowEchelonForm(products, columns, columns, IMPLIED_EPSILON, pivotColumns);

    final var implied = new BitSet(numberOfTotals);
    implied.set(0, numberOfTotals);
    for (int pivot = 0; pivot < rank; ++pivot) {
      if (pivotColumns[pivot] > 0) {
        implied.clear(pivotColumns[pivot] - 1);
      }
    }
    return implied;
  }
}
