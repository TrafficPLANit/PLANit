package org.goplanit.algorithms.sampling;

import org.goplanit.utils.math.Precision;
import org.goplanit.utils.math.RowEchelonUtils;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * The cube method of Deville and Tillé (2004) is named after a picture. Give every unit, such as a household, an axis
 * of its own, and every possible sample is a corner of a cube: 1 on a unit's axis when it is kept, 0 when it is not.
 * The units' chances, how likely each is to be kept, are a point inside the cube. Each total to keep, such as the kept
 * households' car trips, is a flat plane through that point, holding every sample whose total is right, so the
 * balanced samples are the corners that lie on all the planes. With two households at a chance of a half each, the
 * cube is a square with corners (0, 0), (1, 0), (0, 1) and (1, 1), the chances are the point (0.5, 0.5), and keeping
 * exactly one household is the line through it from (1, 0) to (0, 1). All terms are listed in
 * {@link org.goplanit.algorithms.sampling}.
 * <p>
 * The flight is the first of the method's two phases. It moves the point from the chances towards a corner, a step at a
 * time and always within the planes, so every total stays as it is. Each step goes until it reaches a side of the cube,
 * where one unit's chance becomes 0 or 1 and that unit is decided. Which way a step goes is drawn so that on average
 * the point stays where it was, so each unit is still kept with exactly its chance. In the square there is one step, to
 * (1, 0) or to (0, 1), each half the time. With many units the planes run out of room before a corner is reached: the
 * flight ends with a few units undecided, and the second phase, the landing, gives up totals until those can be
 * settled too.
 * </p>
 * <p>
 * This class runs the flight's steps. It is given the units' chances, which it moves in place, and on each call the
 * units to move, in their order, and how many of the totals to keep: the first ones of the totals over all units and of
 * the totals per stratum, besides each stratum's number of units, which it always keeps. It stops when no step is left
 * and leaves the units it could not decide with the chances they have reached.
 * </p>
 * <p>
 * Only {@link BalancedSampler} uses it, in each of its passes: within each stratum, within runs of strata side by side
 * and over all strata, each pass handing on the units the one before left undecided. The landing uses it too: each
 * time a total is given up it runs the same steps again with one total fewer, so the landing keeps each unit's chance
 * like the flight does.
 * </p>
 * <p>
 * A step must change the chances so that every total stays exactly as it is, bring at least one unit to 0 or 1, and
 * leave the expected chance of each unit unchanged. The first makes the sample balanced, the second makes the steps
 * end, and the third keeps each unit kept with exactly its chance. Looking for such a change over all units at once
 * would take a matrix as wide as the population. The fast form looks only at a window of one more unit than there are
 * totals, which always leaves a change that keeps the totals, and so costs the same however many units there are.
 * </p>
 * <p>
 * A step, as in Deville and Tillé (2004), on a window as in the fast form of Chauvet and Tillé (2006):
 * </p>
 * <ol>
 * <li>The window holds undecided units, in their order, until there are more of them than the totals it keeps.</li>
 * <li>A direction: changes to the window's chances that leave every total unchanged, a vector in the null space of the
 * matrix of the window's values, one row per total and one column per unit.</li>
 * <li>The longest moves along the direction and against it that keep every chance from 0 to 1. The step takes the
 * move along with a chance of the move against's length over the two lengths together, otherwise the move against, so
 * the expected change is nought. Either way a unit reaches 0 or 1 and leaves the window, and the next undecided units
 * come in.</li>
 * </ol>
 * <p>
 * With strata, the window keeps, besides the totals over all units, each stratum's number of units and totals per
 * stratum for the strata its units are in, so it needs one more unit than all those rows together, as Algorithm 1 of
 * Jauslin, Eustache and Tillé (2021) works out. What differs here:
 * </p>
 * <ul>
 * <li>The totals per stratum are additions, as the papers keep only the number of units per stratum.</li>
 * <li>A stratum counts for as many rows as are independent, at most its number of rows, and at most its number of
 * units in the window. A stratum all of whose undecided units are in the window and whose own rows leave them no
 * freedom, such as two households of different sizes that must keep their zone's persons, is set aside, its units left
 * undecided for the landing. No direction could move them, yet they would hold the window open to the end. Hasler
 * and Tillé (2014) take a stratum out once all its units are decided; this also takes out one whose units cannot
 * move.</li>
 * <li>The direction is found by Gaussian elimination with partial pivoting, {@link RowEchelonUtils}, each row first
 * scaled so its largest value is 1, so one tolerance suits values of any size; it is worked out afresh each step on an
 * array kept from step to step, so a step allocates nothing.</li>
 * <li>A chance within 10<sup>-9</sup> of 0 or 1 counts as decided, so rounding errors cannot hold a unit in the
 * window.</li>
 * <li>The totals over all units implied by the count and the totals before them, see
 * {@link BalancedSamplingUnits#collectImpliedTotals()}, are left out of the window's matrix and of its count of rows:
 * they are kept whenever the others are, and each would add a row to every step.</li>
 * </ul>
 *
 * @author markr
 */
final class CubeFlight {

  /** a chance this close to 0 or 1 counts as decided */
  private static final double DECIDED_EPSILON = Precision.EPSILON_9;

  /** a value of a scaled row below this counts as 0 when looking for a pivot */
  private static final double PIVOT_EPSILON = Precision.EPSILON_9;

  /** the units, with their values */
  private final BalancedSamplingUnits units;

  /** current chance of each unit, moved by the steps */
  private final double[] chances;

  /** the totals over all units a step writes rows for, in their order: those not implied by the ones before them */
  private final int[] independentTotals;

  /** number of totals over all units kept in the current flight, the first ones of the list */
  private int activeTotals;

  /** number of the independent totals among those kept in the current flight, the first ones of independentTotals */
  private int activeIndependentTotals;

  /** number of totals per stratum kept in the current flight, the first ones of the list */
  private int activeStratumTotals;

  /** units in the window, in their order, so a stratum's units are next to each other */
  private int[] windowUnits;

  /** number of units in the window */
  private int windowSize;

  /** per stratum in the window, the stratum */
  private int[] segmentStratum;

  /** per stratum in the window, the position of its first unit in the window */
  private int[] segmentStart;

  /** per stratum in the window, its number of units in the window */
  private int[] segmentLength;

  /** per stratum in the window, whether all its undecided units have come in */
  private boolean[] segmentComplete;

  /** per stratum in the window with all its units in, the number of independent rows among its own */
  private int[] segmentRank;

  /** number of strata in the window */
  private int segmentCount;

  /** the matrix of a step, row after row, rebuilt each step */
  private double[] matrix;

  /** per row of the matrix in row echelon form, the column of its pivot */
  private int[] pivotColumns;

  /** the direction of the step, per unit in the window */
  private double[] direction;

  /**
   * Make room for at least the given number of units in the window
   *
   * @param size to make room for
   */
  private void makeRoomForWindowUnits(int size) {
    if (size > windowUnits.length) {
      int length = Math.max(size, 2 * windowUnits.length);
      windowUnits = Arrays.copyOf(windowUnits, length);
      pivotColumns = Arrays.copyOf(pivotColumns, length);
      direction = Arrays.copyOf(direction, length);
    }
  }

  /**
   * Make room for at least the given number of strata in the window
   *
   * @param count to make room for
   */
  private void makeRoomForSegments(int count) {
    if (count > segmentStratum.length) {
      int length = Math.max(count, 2 * segmentStratum.length);
      segmentStratum = Arrays.copyOf(segmentStratum, length);
      segmentStart = Arrays.copyOf(segmentStart, length);
      segmentLength = Arrays.copyOf(segmentLength, length);
      segmentComplete = Arrays.copyOf(segmentComplete, length);
      segmentRank = Arrays.copyOf(segmentRank, length);
    }
  }

  /**
   * Make room for at least the given number of values in the matrix
   *
   * @param size to make room for
   */
  private void makeRoomForMatrixValues(int size) {
    if (size > matrix.length) {
      matrix = new double[Math.max(size, 2 * matrix.length)];
    }
  }

  /**
   * Scale a row of the matrix so its largest value is 1
   *
   * @param at position of the row's first value
   * @param columns number of values in the row
   * @param largest the largest of the row's values, without sign
   * @return true when the row has a value other than 0, false when it is all 0 and so keeps nothing
   */
  private boolean scaleRow(int at, int columns, double largest) {
    if (largest == 0) {
      return false;
    }
    for (int column = 0; column < columns; ++column) {
      matrix[at + column] /= largest;
    }
    return true;
  }

  /**
   * Write the rows of a stratum in the window into the matrix: its number of units, a 1 for each of its units, and
   * its active totals per stratum, its units' values, all other columns 0. A row of 0 only is left out
   * <p>
   * Each row is a plane the step must stay on, for this stratum alone. The row of 1s keeps the sum of its units'
   * chances, so the stratum ends with its number of units, and each total per stratum keeps that total within it. The
   * other strata's columns are 0, as these rows say nothing about their units. For example, a zone with three households
   * of 2, 3 and 1 persons in the window gets the rows 1, 1, 1 and 2, 3, 1, the second scaled to 0.67, 1, 0.33 so its
   * largest value is 1.
   * </p>
   *
   * @param segment the stratum's position among the strata in the window
   * @param row of the matrix to write its first row to
   * @param firstColumn column of the stratum's first unit
   * @param columns number of columns of the matrix
   * @return the number of rows written
   */
  private int writeStratumRows(int segment, int row, int firstColumn, int columns) {
    final int start = segmentStart[segment];
    final int length = segmentLength[segment];
    int nextRow = row;
    int at = nextRow * columns;
    Arrays.fill(matrix, at, at + columns, 0.0);
    Arrays.fill(matrix, at + firstColumn, at + firstColumn + length, 1.0);
    ++nextRow;
    for (int stratumTotal = 0; stratumTotal < activeStratumTotals; ++stratumTotal) {
      at = nextRow * columns;
      Arrays.fill(matrix, at, at + columns, 0.0);
      double largest = 0;
      for (int offset = 0; offset < length; ++offset) {
        double value = units.getStratumValue(windowUnits[start + offset], stratumTotal);
        matrix[at + firstColumn + offset] = value;
        largest = Math.max(largest, Math.abs(value));
      }
      if (scaleRow(at, columns, largest)) {
        ++nextRow;
      }
    }
    return nextRow - row;
  }

  /**
   * Write the matrix of the window: a row per active independent total over all units, then the rows of each stratum in
   * the window, a column per unit in the window, each row scaled so its largest value is 1
   * <p>
   * The totals mix values of very different sizes, a count of 0 or 1 beside kilometres in the hundreds, and whether a
   * value counts as 0 during the elimination must not depend on that size. Scaled rows let one tolerance decide for
   * all of them. A row of 0 only, such as a count none of the window's units has, keeps nothing and is left out, which
   * makes the elimination smaller.
   * </p>
   *
   * @return the number of rows
   */
  private int writeWindowMatrix() {
    final int columns = windowSize;
    makeRoomForMatrixValues((activeIndependentTotals + segmentCount * (1 + activeStratumTotals)) * columns);
    int row = 0;
    for (int independent = 0; independent < activeIndependentTotals; ++independent) {
      final int total = independentTotals[independent];
      int at = row * columns;
      double largest = 0;
      for (int column = 0; column < columns; ++column) {
        double value = units.getValue(windowUnits[column], total);
        matrix[at + column] = value;
        largest = Math.max(largest, Math.abs(value));
      }
      if (scaleRow(at, columns, largest)) {
        ++row;
      }
    }
    for (int segment = 0; segment < segmentCount; ++segment) {
      row += writeStratumRows(segment, row, segmentStart[segment], columns);
    }
    return row;
  }

  /**
   * Find a direction for the window: a change to its units' chances that leaves every row of its matrix unchanged, the
   * vector of the null space with 1 for the first column without a pivot, see {@link RowEchelonUtils}
   * <p>
   * Any direction in the null space keeps the totals and the chances, so the choice only decides which units move.
   * The first free column moves the units earliest in the window, those that came in first, so units are settled
   * roughly in their order, as in Chauvet and Tillé (2006). For example, with a count row of 1, 1, 1 and a value row of
   * 1, 2, 3, the third column is free and the direction is 1, -2, 1: the count and the value stay the same.
   * </p>
   *
   * @return true when found, in {@link #direction}, false when the matrix leaves no direction
   */
  private boolean findDirection() {
    final int columns = windowSize;
    final int rows = writeWindowMatrix();
    final int rank = RowEchelonUtils.reduceToRowEchelonForm(matrix, rows, columns, PIVOT_EPSILON, pivotColumns);
    return RowEchelonUtils.findNullSpaceVector(matrix, columns, rank, pivotColumns, direction);
  }

  /**
   * Move the window's chances along the direction or against it, the longest move that keeps every chance from 0 to
   * 1, along it with a chance of the move against's length over both lengths together, so the expected change of each
   * chance is nought. The unit that limits the move lands on 0 or 1 exactly, and any other within the tolerance of
   * them is put on them
   * <p>
   * This is the random move of Deville and Tillé (2004), and the reason each unit ends up kept with exactly its
   * chance. With a move along of 0.2 and a move against of 0.6, the step goes along three times in four and against
   * once, and three times 0.2 less once 0.6 is nought. As both moves go as far as they can, one unit always reaches 0
   * or 1, so the steps end.
   * </p>
   *
   * @param random to draw the way to move from
   */
  private void move(SplittableRandom random) {
    double alongLength = Double.POSITIVE_INFINITY;
    double againstLength = Double.POSITIVE_INFINITY;
    int alongLimit = -1;
    int againstLimit = -1;
    for (int column = 0; column < windowSize; ++column) {
      double change = direction[column];
      if (change == 0) {
        continue;
      }
      double chance = chances[windowUnits[column]];
      double toOne = (1 - chance) / Math.abs(change);
      double toZero = chance / Math.abs(change);
      double along = change > 0 ? toOne : toZero;
      double against = change > 0 ? toZero : toOne;
      if (along < alongLength) {
        alongLength = along;
        alongLimit = column;
      }
      if (against < againstLength) {
        againstLength = against;
        againstLimit = column;
      }
    }
    final boolean moveAlong = random.nextDouble() * (alongLength + againstLength) < againstLength;
    final double step = moveAlong ? alongLength : -againstLength;
    for (int column = 0; column < windowSize; ++column) {
      if (direction[column] == 0) {
        continue;
      }
      int unit = windowUnits[column];
      double chance = chances[unit] + step * direction[column];
      if (Precision.smallerEqual(chance, 0, DECIDED_EPSILON)) {
        chance = 0;
      } else if (Precision.greaterEqual(chance, 1, DECIDED_EPSILON)) {
        chance = 1;
      }
      chances[unit] = chance;
    }
    final int limit = moveAlong ? alongLimit : againstLimit;
    chances[windowUnits[limit]] = (direction[limit] > 0) == moveAlong ? 1 : 0;
  }

  /**
   * Work out the independent rows of a stratum in the window with all its undecided units in. With more units than
   * rows, the rows count in full
   *
   * @param segment the stratum's position among the strata in the window
   * @return true when its own rows leave its units no freedom, so no direction can move them
   */
  private boolean settleStratumRank(int segment) {
    final int length = segmentLength[segment];
    final int rowsPerStratum = 1 + activeStratumTotals;
    if (length > rowsPerStratum) {
      segmentRank[segment] = rowsPerStratum;
      return false;
    }
    makeRoomForMatrixValues(rowsPerStratum * length);
    final int rows = writeStratumRows(segment, 0, 0, length);
    segmentRank[segment] = RowEchelonUtils.reduceToRowEchelonForm(matrix, rows, length, PIVOT_EPSILON, pivotColumns);
    return segmentRank[segment] == length;
  }

  /**
   * Whether the window has more units than independent rows, so a direction exists. A stratum whose units have not all
   * come in counts for its number of rows, or its number of units in the window when fewer
   *
   * @return true when the window certainly has a direction
   */
  private boolean hasCertainDirection() {
    int rows = activeIndependentTotals;
    for (int segment = 0; segment < segmentCount; ++segment) {
      rows += segmentComplete[segment] ? segmentRank[segment]
          : Math.min(1 + activeStratumTotals, segmentLength[segment]);
    }
    return windowSize > rows;
  }

  /**
   * Add a unit to the window, after its last unit, starting a new stratum in the window when it is of another
   * stratum than that unit
   *
   * @param unit to add
   */
  private void addToWindow(int unit) {
    final int stratum = units.getStratum(unit);
    if (segmentCount == 0 || segmentStratum[segmentCount - 1] != stratum) {
      makeRoomForSegments(segmentCount + 1);
      segmentStratum[segmentCount] = stratum;
      segmentStart[segmentCount] = windowSize;
      segmentLength[segmentCount] = 0;
      segmentComplete[segmentCount] = false;
      ++segmentCount;
    }
    makeRoomForWindowUnits(windowSize + 1);
    windowUnits[windowSize++] = unit;
    ++segmentLength[segmentCount - 1];
  }

  /**
   * Mark the last stratum in the window as having all its undecided units in, and set it aside when its own rows leave
   * them no freedom
   */
  private void completeLastStratum() {
    final int last = segmentCount - 1;
    segmentComplete[last] = true;
    if (settleStratumRank(last)) {
      windowSize = segmentStart[last];
      segmentCount = last;
    }
  }

  /**
   * Fill the window with the next undecided units until it certainly has a direction or the units run out
   *
   * @param order of the units, a stratum's units next to each other
   * @param next position in the order of the next unit to look at
   * @param end position in the order after the last unit
   * @param addAtLeastOne when true, add one undecided unit even when the window already has a direction
   * @return the position in the order of the next unit to look at
   */
  private int fillWindow(int[] order, int next, int end, boolean addAtLeastOne) {
    boolean addOne = addAtLeastOne;
    while (next < end && (addOne || !hasCertainDirection())) {
      int unit = order[next++];
      if (!isDecided(chances[unit])) {
        addToWindow(unit);
        addOne = false;
      }
      boolean lastStratumIncomplete = segmentCount > 0 && !segmentComplete[segmentCount - 1];
      if (lastStratumIncomplete
          && (next == end || units.getStratum(order[next]) != segmentStratum[segmentCount - 1])) {
        completeLastStratum();
      }
    }
    return next;
  }

  /**
   * Take the units decided by the last step out of the window, and set aside each stratum with all its units in whose
   * own rows leave the units still in no freedom
   * <p>
   * A stratum's own rows are its number of units and its totals per stratum. Once a step leaves it, say, two units of
   * different sizes that must keep the stratum's count and size, no direction can move either, whatever the other
   * units do. Left in, the stratum would add two units and two rows to every later step without ever moving, and with
   * thousands of strata the window would grow without end. Set aside, its units wait for the landing, which gives up
   * the totals per stratum.
   * </p>
   */
  private void removeDecidedFromWindow() {
    int kept = 0;
    int keptSegments = 0;
    for (int segment = 0; segment < segmentCount; ++segment) {
      final int start = kept;
      final int end = segmentStart[segment] + segmentLength[segment];
      kept = moveUndecidedForward(windowUnits, segmentStart[segment], end, kept, chances);
      final int length = kept - start;
      if (length == 0) {
        continue;
      }
      final boolean changed = length != segmentLength[segment];
      segmentStratum[keptSegments] = segmentStratum[segment];
      segmentStart[keptSegments] = start;
      segmentLength[keptSegments] = length;
      segmentComplete[keptSegments] = segmentComplete[segment];
      segmentRank[keptSegments] = segmentRank[segment];
      if (changed && segmentComplete[keptSegments] && settleStratumRank(keptSegments)) {
        kept = start;
        continue;
      }
      ++keptSegments;
    }
    windowSize = kept;
    segmentCount = keptSegments;
  }

  /**
   * Whether a chance counts as decided, within the tolerance of 0 or 1
   *
   * @param chance to check
   * @return true when decided
   */
  public static boolean isDecided(double chance) {
    return Precision.smallerEqual(chance, 0, DECIDED_EPSILON) || Precision.greaterEqual(chance, 1, DECIDED_EPSILON);
  }

  /**
   * Move the undecided units of a range forward to a position, keeping their order; what is left behind them is not to
   * be used
   *
   * @param units holding the range
   * @param from position of the range's first unit
   * @param end position after the range's last unit
   * @param to position the first undecided unit moves to, at most from
   * @param chances current chance of each unit
   * @return the position after the last undecided unit moved
   */
  public static int moveUndecidedForward(int[] units, int from, int end, int to, double[] chances) {
    for (int position = from; position < end; ++position) {
      if (!isDecided(chances[units[position]])) {
        units[to++] = units[position];
      }
    }
    return to;
  }

  /**
   * Constructor
   *
   * @param units to move the chances of
   * @param chances current chance of each unit, moved in place
   * @param independentTotals the totals over all units to write rows for, in their order, those not implied by the
   *                          count and the totals before them
   */
  public CubeFlight(BalancedSamplingUnits units, double[] chances, int[] independentTotals) {
    this.units = units;
    this.chances = chances;
    this.independentTotals = independentTotals;
    int windowLength = 2 * (independentTotals.length + units.getNumberOfStratumTotals() + 2);
    this.windowUnits = new int[windowLength];
    this.pivotColumns = new int[windowLength];
    this.direction = new double[windowLength];
    this.segmentStratum = new int[8];
    this.segmentStart = new int[8];
    this.segmentLength = new int[8];
    this.segmentComplete = new boolean[8];
    this.segmentRank = new int[8];
    this.matrix = new double[windowLength * windowLength];
  }

  /**
   * Fly: move the chances of the given units in steps, each keeping the first totals over all units, the first totals
   * per stratum and each stratum's number of units, until no step can. A stratum's units must be next to each other in
   * the order. Units left undecided are those no step could move
   * <p>
   * One flight is one pass of the sampler, over one stratum, a run of strata or all of them, with the totals that pass
   * keeps. The window takes in units as the earlier ones are decided, so the work of a step stays the same however many
   * units the flight covers, and a flight of a million units costs a million small steps. It ends when the window has no
   * direction and no unit is left to take in; what it leaves undecided, at most about as many units as it keeps rows,
   * goes to the next pass, which keeps fewer totals or takes in more strata. For example, a zone of 780 households with
   * 120 rows to keep leaves at most about 120 of them to the pass over all zones.
   * </p>
   *
   * @param order of the units to move, a stratum's units next to each other
   * @param from position in the order of the first unit
   * @param to position in the order after the last unit
   * @param totals number of totals over all units to keep, the first ones of the list
   * @param stratumTotals number of totals per stratum to keep, the first ones of the list
   * @param random to draw the moves from
   */
  public void fly(int[] order, int from, int to, int totals, int stratumTotals, SplittableRandom random) {
    activeTotals = totals;
    activeStratumTotals = stratumTotals;
    activeIndependentTotals = 0;
    while (activeIndependentTotals < independentTotals.length
        && independentTotals[activeIndependentTotals] < activeTotals) {
      ++activeIndependentTotals;
    }
    windowSize = 0;
    segmentCount = 0;
    int next = from;
    boolean addAtLeastOne = false;
    while (true) {
      next = fillWindow(order, next, to, addAtLeastOne);
      if (windowSize > 0 && findDirection()) {
        move(random);
        removeDecidedFromWindow();
        addAtLeastOne = false;
      } else if (next < to) {
        /* rounding errors left no direction where there should be one: let the window take in more units */
        addAtLeastOne = true;
      } else {
        return;
      }
    }
  }
}
