package org.goplanit.demands.discrete.trip;

import org.goplanit.utils.functionalinterface.TriFunction;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrixUtils;

import java.util.Map;

/**
 * Utilities that measure a trip by data given beside it, such as its distance from the skim of its mode
 * <p>
 * A trip knows its mode, its zones and its tour; what it is measured by, such as a distance skim per mode, is made and
 * held elsewhere, by a skim builder or a reader, and dropped once used. Reading it here keeps that knowledge out of
 * the trip, so a trip depends on nothing it does not hold.
 * </p>
 *
 * @author markr
 */
public final class TripUtils {

  /**
   * The skim cell of a trip's mode between its origin and destination zone, its tour's where it has none of its own,
   * read through the given reader
   *
   * @param trip to read the cell of
   * @param distanceSkims distance skim of each mode
   * @param readCell reads the value of the skim's cell of the trip's origin and destination zone
   * @return the value read, NaN when the trip has no mode, origin or destination, or its mode no skim
   */
  private static double readSkimCell(Trip trip, Map<Mode, OdSkimMatrix> distanceSkims,
      TriFunction<OdSkimMatrix, OdZone, OdZone, Double> readCell) {
    var origin = trip.getOrigin(true);
    var destination = trip.getDestination(true);
    var skim = trip.getMode() == null ? null : distanceSkims.get(trip.getMode());
    return skim == null || origin == null || destination == null ? Double.NaN :
        readCell.apply(skim, origin, destination);
  }

  /** utilities, not to be instantiated */
  private TripUtils() {
  }

  /**
   * The distance of a trip by its mode between its origin and destination zone, its tour's where it has none of its
   * own, as its mode's skim holds it, in the skim's own unit
   * <p>
   * Read without a check of the unit or a conversion, for a loop over many trips that checked beforehand that every
   * skim is in the unit it needs, such as through {@link OdSkimMatrixUtils#areInUnit}; otherwise
   * {@link #getDistance(Trip, Map, Unit)} reads in a given unit.
   * </p>
   *
   * @param trip to measure
   * @param distanceSkims distance skim of each mode, every cell with a value, such as those of
   *                      {@link OdSkimMatrixUtils} with the cells the search cannot reach inferred
   * @return the distance in the unit of the skim, NaN when the trip has no mode, origin or destination, or its mode no
   * skim
   */
  public static double getDistance(Trip trip, Map<Mode, OdSkimMatrix> distanceSkims) {
    return readSkimCell(trip, distanceSkims, OdSkimMatrix::getValue);
  }

  /**
   * The distance of a trip by its mode between its origin and destination zone, its tour's where it has none of its
   * own, in the given unit whatever unit its mode's skim is in
   *
   * @param trip to measure
   * @param distanceSkims distance skim of each mode, in any distance unit, every cell with a value, such as those of
   *                      {@link OdSkimMatrixUtils} with the cells the search cannot reach inferred
   * @param distanceUnit to give the distance in
   * @return the distance in the given unit, NaN when the trip has no mode, origin or destination, or its mode no skim
   */
  public static double getDistance(Trip trip, Map<Mode, OdSkimMatrix> distanceSkims, Unit distanceUnit) {
    return readSkimCell(trip, distanceSkims,
        (skim, origin, destination) -> skim.getValueConvertedTo(origin, destination, distanceUnit));
  }
}
