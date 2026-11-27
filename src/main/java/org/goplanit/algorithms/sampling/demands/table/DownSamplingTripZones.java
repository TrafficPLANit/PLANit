package org.goplanit.algorithms.sampling.demands.table;

import org.goplanit.algorithms.sampling.demands.enums.DownSamplingModeGroup;

import java.util.Arrays;

/**
 * The zones of each person trip of the sampling units of a {@link DiscreteDemandsDownSamplingTable}: per person trip
 * its origin zone, its destination zone and its mode group, the zones by their position along the curve, a row per
 * sampling unit
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a zone's <b>position along the
 * curve</b> is its place in the order of a Hilbert curve, see {@link org.goplanit.utils.geo.HilbertCurveUtils}, so
 * zones next to each other in that order lie close together; an <b>area</b> is a run of zones next to each other in
 * that order; a <b>sampling unit</b> is a household with its persons, tours and trips, kept or dropped whole.
 * </p>
 * <p>
 * The trips between areas can only be counted once the areas are binned, after every sampling unit is counted, while
 * the down-sampling table counts each sampling unit as it comes. A count per pair of zones would be far too many
 * categories, 3,378 by 3,378 by five mode groups for the GMA; the two zones and the mode group of a person trip take
 * 9 bytes, about 0.19 GB for the GMA's 21 million person trips. The rows are filled one at a time, as the down-sampling
 * table's other values are.
 * </p>
 *
 * @author markr
 */
final class DownSamplingTripZones {

  /** person trips and rows there is room for at the start */
  private static final int INITIAL_LENGTH = 1024;

  /** the first person trip of each row, and one past the last person trip of the last row */
  private int[] rowStarts = new int[INITIAL_LENGTH];

  /** number of rows finished */
  private int numberOfRows;

  /** origin zone position along the curve of each person trip, -1 for a zone without a place */
  private int[] origins = new int[INITIAL_LENGTH];

  /** destination zone position along the curve of each person trip, -1 for a zone without a place */
  private int[] destinations = new int[INITIAL_LENGTH];

  /** mode group of each person trip, by its ordinal */
  private byte[] modeGroups = new byte[INITIAL_LENGTH];

  /** number of person trips added */
  private int numberOfTrips;

  /**
   * The area cell of a person trip: its mode group, then its origin area, then its destination area
   *
   * @param trip index of the person trip, from 0
   * @param areaOfPosition the area of each zone position along the curve
   * @param areaSlots the number of areas, and one more for an area not known, the last
   * @return the cell, from 0 up to the number of mode groups times the area slots squared
   */
  private int findCell(int trip, int[] areaOfPosition, int areaSlots) {
    int originArea = origins[trip] < 0 ? areaSlots - 1 : areaOfPosition[origins[trip]];
    int destinationArea = destinations[trip] < 0 ? areaSlots - 1 : areaOfPosition[destinations[trip]];
    return (modeGroups[trip] * areaSlots + originArea) * areaSlots + destinationArea;
  }

  /**
   * Make room for at least the given number of person trips, growing by half
   *
   * @param count to make room for
   */
  private void makeRoomForTrips(int count) {
    if (count > origins.length) {
      int length = Math.max(count, origins.length + (origins.length >> 1));
      origins = Arrays.copyOf(origins, length);
      destinations = Arrays.copyOf(destinations, length);
      modeGroups = Arrays.copyOf(modeGroups, length);
    }
  }

  /**
   * Make room for the start of at least the given number of rows, growing by half
   *
   * @param count to make room for
   */
  private void makeRoomForRows(int count) {
    if (count > rowStarts.length) {
      rowStarts = Arrays.copyOf(rowStarts, Math.max(count, rowStarts.length + (rowStarts.length >> 1)));
    }
  }

  /**
   * Add a person trip to the row being filled
   *
   * @param originPosition of the origin zone along the curve, -1 for a zone without a place
   * @param destinationPosition of the destination zone along the curve, -1 for a zone without a place
   * @param modeGroup of the person trip
   */
  public void add(int originPosition, int destinationPosition, DownSamplingModeGroup modeGroup) {
    makeRoomForTrips(numberOfTrips + 1);
    origins[numberOfTrips] = originPosition;
    destinations[numberOfTrips] = destinationPosition;
    modeGroups[numberOfTrips] = (byte) modeGroup.ordinal();
    ++numberOfTrips;
  }

  /** Finish the row being filled, so the next one can be filled */
  public void finishRow() {
    makeRoomForRows(numberOfRows + 2);
    rowStarts[++numberOfRows] = numberOfTrips;
  }

  /**
   * The person trips of every row in each area cell, see {@link #addRowTo(int, int[], int, int[], double[])}
   *
   * @param areaOfPosition the area of each zone position along the curve
   * @param areaSlots the number of areas, and one more for an area not known, the last
   * @return the person trips of each cell
   */
  public long[] collectCellTotals(int[] areaOfPosition, int areaSlots) {
    var totals = new long[DownSamplingModeGroup.values().length * areaSlots * areaSlots];
    for (int trip = 0; trip < numberOfTrips; ++trip) {
      ++totals[findCell(trip, areaOfPosition, areaSlots)];
    }
    return totals;
  }

  /**
   * Add the person trips of a row to the columns of their area cells
   *
   * @param row finished, from 0
   * @param areaOfPosition the area of each zone position along the curve
   * @param areaSlots the number of areas, and one more for an area not known, the last
   * @param columnOfCell the column of each area cell, -1 for a cell left out
   * @param target to add to, by column
   */
  public void addRowTo(int row, int[] areaOfPosition, int areaSlots, int[] columnOfCell, double[] target) {
    for (int trip = rowStarts[row]; trip < rowStarts[row + 1]; ++trip) {
      int column = columnOfCell[findCell(trip, areaOfPosition, areaSlots)];
      if (column >= 0) {
        ++target[column];
      }
    }
  }
}
