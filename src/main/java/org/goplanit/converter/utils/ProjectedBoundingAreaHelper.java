package org.goplanit.converter.utils;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.utils.epsg.ProjectedEpsgCodesByCountry;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.geo.PlanitGeometryOperationUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.zoning.TransferZone;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.operation.distance.IndexedFacetDistance;
import java.util.logging.Logger;

/**
 * Utilities for projected bounding area instances for fast spatial checks assistance
 * readers
 */
public class ProjectedBoundingAreaHelper {

  private static final Logger LOGGER = Logger.getLogger(ProjectedBoundingAreaHelper.class.getCanonicalName());

  /** spatially indexed version of bounding polygon if any for quick comparisons */
  private final PreparedPolygon preppedBoundingPolygonWgs84;

  /** be able to transform from source to projected destination Crs */
  private final MathTransform mathTransformSourceToProjection;

  /** indexed distance facet for fast calculating of distances to bounding polygon in projected CRS,
   * make sure any calcs feed in geometries that are also projected so NOT Wgs84 */
  private final IndexedFacetDistance indexedBoundingPolygonDistProjected;

  /** leniency to apply for water based checks */
  protected final double maximumDistanceWaterBasedOutsideBoundingPolygonInMeters;

  /** default distance outside a bounding polygon for water based entities */
  public static double DEFAULT_MAX_FERRY_DISTANCE_OUTSIDE_BOUNDING_AREA_M = 2_000;

  public ProjectedBoundingAreaHelper(){
    preppedBoundingPolygonWgs84 = null;
    mathTransformSourceToProjection = null;
    indexedBoundingPolygonDistProjected = null;
    maximumDistanceWaterBasedOutsideBoundingPolygonInMeters = DEFAULT_MAX_FERRY_DISTANCE_OUTSIDE_BOUNDING_AREA_M;
  }

  /**
   * Constructor
   *
   * @param boundingPolygon to consider
   * @param originalCrs to consider
   * @param destinationCountryName to consider
   * @param maximumDistanceFerryOutsideBoundingPolygonInMeters to use for water leniency
   */
  public ProjectedBoundingAreaHelper(
      Polygon boundingPolygon,
      CoordinateReferenceSystem originalCrs,
      String destinationCountryName,
      double maximumDistanceFerryOutsideBoundingPolygonInMeters ){
    // prepare polygon for faster checks
    this.preppedBoundingPolygonWgs84 = PlanitGeometryOperationUtils.extractPreparedPolygonForQuickSpatialComparisons(
        boundingPolygon);
    // prepare indexed distance faced for fast distance to calcs (in projection so it is not in degrees)
    var projectedCrs =
        PlanitCrsUtils.createCoordinateReferenceSystem(ProjectedEpsgCodesByCountry.getEpsg(destinationCountryName));
    this.mathTransformSourceToProjection = PlanitJtsUtils.findMathTransform(originalCrs, projectedCrs);
    var projectedBoundingPolygon = PlanitJtsUtils.transformGeometrySafe(
        boundingPolygon,mathTransformSourceToProjection);
    this.indexedBoundingPolygonDistProjected = new IndexedFacetDistance(projectedBoundingPolygon);

    this.maximumDistanceWaterBasedOutsideBoundingPolygonInMeters = maximumDistanceFerryOutsideBoundingPolygonInMeters;
  }

  /**
   * Factory method
   * @param boundingPolygon to consider
   * @param originalCrs to consider
   * @param destinationCountryName to consider
   * @param maximumDistanceFerryOutsideBoundingPolygonInMeters to consider
   * @return helper created
   */
  public static ProjectedBoundingAreaHelper of(
      Polygon boundingPolygon,
      CoordinateReferenceSystem originalCrs,
      String destinationCountryName,
      double maximumDistanceFerryOutsideBoundingPolygonInMeters) {
    return new ProjectedBoundingAreaHelper(
        boundingPolygon, originalCrs, destinationCountryName, maximumDistanceFerryOutsideBoundingPolygonInMeters);
  }

  /**
   * Factory method for empty instance
   * @return empty instance
   */
  public static ProjectedBoundingAreaHelper empty() {
    return new ProjectedBoundingAreaHelper();
  }

  public boolean isEmpty(){
    return preppedBoundingPolygonWgs84 == null;
  }

  public PreparedPolygon getPreparedBoundingPolygon(){
    return preppedBoundingPolygonWgs84;
  }

  /**
   * Calculate distance to bounding polygon (assumes one is present otherwise undefined behaviour) for a
   * given point
   * @param point to calculate distance to bounding polygon
   * @param applyProjection when true transform point to projection (destination Crs of converter assumed to
   *                        be a projected), when false it is assumed to already be projected and calculated as is
   * @return distance in destination Crs units (typically meters)
   */
  public double calculateProjectedDistanceToBoundingPolygon(Point point, boolean applyProjection){
    return indexedBoundingPolygonDistProjected.distance(
        applyProjection ? PlanitJtsUtils.transformGeometrySafe(point, mathTransformSourceToProjection): point);
  }

  /**
   * Check if within bounding area if specified and use lenience for water based infra if so configured
   *
   * @param transferZone            to check
   * @param useWaterLeniency flag to use water lenience in absence of OSM tags
   * @return true when eligible, false otherwise
   */
  public boolean fallsWithinSpatiallyEligibleBoundingArea(TransferZone transferZone, boolean useWaterLeniency) {
    var geometry = transferZone.getGeometry(true);
    if(!useWaterLeniency){
      return isPartlyOrWhollyWithinBoundaryArea(
          geometry, true);
    }else{
      if(geometry instanceof Point){
        return isNearPartlyOrWhollyWithinBoundaryArea(
            (Point) geometry, maximumDistanceWaterBasedOutsideBoundingPolygonInMeters,true);
      }else if(geometry instanceof LineString){
        return isNearPartlyOrWhollyWithinBoundaryArea(
            (LineString) geometry, maximumDistanceWaterBasedOutsideBoundingPolygonInMeters,true);
      }else{
        LOGGER.warning("Unsupported geometry type for transfer zone found, should not happen");
        return false;
      }
    }
  }

  /**
   * Verify if geometry is (partly) within boundary provided.
   *
   * @param geometry to check
   * @param isWithinWhenNoBoundary when true, true is returned if provided boundary has no polygon defined,
   *                               false otherwise
   * @return true when within boundary, false otherwise
   */
  public boolean isPartlyOrWhollyWithinBoundaryArea(
      Geometry geometry,
      boolean isWithinWhenNoBoundary){
    if(preppedBoundingPolygonWgs84 == null){
      return isWithinWhenNoBoundary;
    }

    return preppedBoundingPolygonWgs84.intersects(geometry);
  }

  /**
   * Verify if geometry is (partly) within boundary provided.
   *
   * @param point to check
   * @param maxProjectedDistanceToBoundary to allow
   * @param isWithinWhenNoBoundary when true, true is returned if provided boundary has no polygon defined,
   *                               false otherwise
   * @return true when within boundary, false otherwise
   */
  public boolean isNearPartlyOrWhollyWithinBoundaryArea(
      Point point,
      double maxProjectedDistanceToBoundary,
      boolean isWithinWhenNoBoundary){
    if(preppedBoundingPolygonWgs84 == null){
      return isWithinWhenNoBoundary;
    }

    boolean success = isPartlyOrWhollyWithinBoundaryArea(point, isWithinWhenNoBoundary);
    if(!success && maxProjectedDistanceToBoundary > 0){
      success = maxProjectedDistanceToBoundary <
          this.calculateProjectedDistanceToBoundingPolygon(point, false);
    }
    return success;
  }

  /**
   * Verify if geometry is (partly) within boundary provided.
   *
   * @param lineString to check
   * @param maxProjectedDistanceToBoundary to allow
   * @param isWithinWhenNoBoundary when true, true is returned if provided boundary has no polygon defined,
   *                               false otherwise
   * @return true when within boundary, false otherwise
   */
  public boolean isNearPartlyOrWhollyWithinBoundaryArea(
      LineString lineString, double maxProjectedDistanceToBoundary, boolean isWithinWhenNoBoundary){

    if(preppedBoundingPolygonWgs84 == null){
      return isWithinWhenNoBoundary;
    }

    boolean success = isPartlyOrWhollyWithinBoundaryArea(lineString, isWithinWhenNoBoundary);
    if(!success && maxProjectedDistanceToBoundary > 0){
      for(int index=0; index < lineString.getNumPoints();++index){
        var currPoint = lineString.getPointN(index);
        success = maxProjectedDistanceToBoundary <
            this.calculateProjectedDistanceToBoundingPolygon(currPoint, false);
        if(success){
          break;
        }
      }
    }
    return success;
  }




}
