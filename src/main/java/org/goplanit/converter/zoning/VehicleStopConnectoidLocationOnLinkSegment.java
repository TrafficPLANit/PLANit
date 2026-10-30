package org.goplanit.converter.zoning;

import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.locationtech.jts.geom.Point;

/**
 * Where a waiting area's stop goes on an access link segment, with how close the places a stop could take on the
 * segment come to the waiting area's centre, to compare segments and links by. For a connectoid location found as the
 * segment's reused end node or its break point, that is the nearest of those two places; otherwise it is the stop
 * location itself. A connectoid location from the fallback check is one the nearest existing point check gave, because
 * the segment offered neither place, see
 * {@link ZoningConverterUtils#findVehicleStopConnectoidLocationForWaitingAreaOnLinkSegment}.
 */
public final class VehicleStopConnectoidLocationOnLinkSegment {

  /** where the stop goes */
  private final Point location;

  /** how close the places a stop could take on the segment come to the waiting area's centre */
  private final double metersFromWaitingAreaCentreToNearestPlace;

  /** whether the location comes from the nearest existing point check because the segment offered no other place */
  private final boolean fromFallbackCheck;

  /**
   * Constructor
   *
   * @param location where the stop goes
   * @param metersFromWaitingAreaCentreToNearestPlace how close the places a stop could take on the segment come to the
   *                                                  waiting area's centre
   * @param fromFallbackCheck whether the location comes from the fallback check
   */
  public VehicleStopConnectoidLocationOnLinkSegment(
      Point location, double metersFromWaitingAreaCentreToNearestPlace, boolean fromFallbackCheck) {
    this.location = location;
    this.metersFromWaitingAreaCentreToNearestPlace = metersFromWaitingAreaCentreToNearestPlace;
    this.fromFallbackCheck = fromFallbackCheck;
  }

  /**
   * A connectoid location whose only place is the location itself
   *
   * @param location where the stop goes, may be null
   * @param waitingAreaCentre to measure from
   * @param fromFallbackCheck whether the location comes from the fallback check
   * @param geoUtils to measure with
   * @return the connectoid location, null when the location is null
   */
  public static VehicleStopConnectoidLocationOnLinkSegment of(
      Point location, Point waitingAreaCentre, boolean fromFallbackCheck, PlanitJtsCrsUtils geoUtils) {
    if (location == null) {
      return null;
    }
    return new VehicleStopConnectoidLocationOnLinkSegment(
        location, geoUtils.getClosestDistanceInMeters(location.getCoordinate(), waitingAreaCentre), fromFallbackCheck);
  }

  /**
   * Where the stop goes
   *
   * @return the location
   */
  public Point getLocation() {
    return location;
  }

  /**
   * How close the places a stop could take on the segment come to the waiting area's centre
   *
   * @return the distance in metres
   */
  public double getMetersFromWaitingAreaCentreToNearestPlace() {
    return metersFromWaitingAreaCentreToNearestPlace;
  }

  /**
   * Whether the location comes from the nearest existing point check because the segment offered no other place
   *
   * @return true when from the fallback check, false otherwise
   */
  public boolean isFromFallbackCheck() {
    return fromFallbackCheck;
  }
}
