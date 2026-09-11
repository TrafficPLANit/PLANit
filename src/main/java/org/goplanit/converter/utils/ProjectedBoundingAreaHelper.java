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
  private final PreparedPolygon preppedBoundingPolygonOriginalCrs;

  /** be able to transform from source to projected destination Crs */
  private final MathTransform mathTransformSourceToProjection;

  /** indexed distance facet for fast calculating of distances to bounding polygon in projected CRS,
   * make sure any calcs feed in geometries that are also projected so NOT Wgs84 */
  private final IndexedFacetDistance indexedBoundingPolygonDistProjected;

  /** leniency to apply for water based checks */
  protected final double maximumDistanceWaterBasedOutsideBoundingPolygonInMeters;

  /** default distance outside a bounding polygon for water based entities */
  public static double DEFAULT_MAX_FERRY_DISTANCE_OUTSIDE_BOUNDING_AREA_M = 2_000;

  protected ProjectedBoundingAreaHelper(){
    preppedBoundingPolygonOriginalCrs = null;
    mathTransformSourceToProjection = null;
    indexedBoundingPolygonDistProjected = null;
    maximumDistanceWaterBasedOutsideBoundingPolygonInMeters = DEFAULT_MAX_FERRY_DISTANCE_OUTSIDE_BOUNDING_AREA_M;
  }

  /**
   * Constructor
   *
   * @param boundingPolygonOriginalCrs to consider
   * @param originalCrs to consider
   * @param destinationCrs to consider
   * @param maximumDistanceFerryOutsideBoundingPolygonInMeters to use for water leniency
   */
  protected ProjectedBoundingAreaHelper(
      Polygon boundingPolygonOriginalCrs,
      CoordinateReferenceSystem originalCrs,
      CoordinateReferenceSystem destinationCrs,
      double maximumDistanceFerryOutsideBoundingPolygonInMeters ){
    // prepare polygon for faster checks
    this.preppedBoundingPolygonOriginalCrs =
        PlanitGeometryOperationUtils.extractPreparedPolygonForQuickSpatialComparisons(boundingPolygonOriginalCrs);
    this.mathTransformSourceToProjection = PlanitJtsUtils.findMathTransform(originalCrs, destinationCrs);
    var projectedBoundingPolygon = PlanitJtsUtils.transformGeometrySafe(
        boundingPolygonOriginalCrs,mathTransformSourceToProjection);
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
    return of(boundingPolygon,
        originalCrs,
        PlanitCrsUtils.createCoordinateReferenceSystem(ProjectedEpsgCodesByCountry.getEpsg(destinationCountryName)),
        maximumDistanceFerryOutsideBoundingPolygonInMeters);
  }

  /**
   * Factory method
   * @param boundingPolygon to consider
   * @param originalCrs to consider
   * @param destinationCrs to consider
   * @param maximumDistanceFerryOutsideBoundingPolygonInMeters to consider
   * @return helper created
   */
  public static ProjectedBoundingAreaHelper of(
      Polygon boundingPolygon,
      CoordinateReferenceSystem originalCrs,
      CoordinateReferenceSystem destinationCrs,
      double maximumDistanceFerryOutsideBoundingPolygonInMeters) {
    return new ProjectedBoundingAreaHelper(
        boundingPolygon, originalCrs, destinationCrs, maximumDistanceFerryOutsideBoundingPolygonInMeters);
  }

  /**
   * Factory method for empty instance
   * @return empty instance
   */
  public static ProjectedBoundingAreaHelper empty() {
    return new ProjectedBoundingAreaHelper();
  }

  public boolean isEmpty(){
    return preppedBoundingPolygonOriginalCrs == null;
  }

  public PreparedPolygon getPreparedBoundingPolygonInOriginalCrs(){
    return preppedBoundingPolygonOriginalCrs;
  }

  /**
   * Calculate distance to bounding polygon (assumes one is present otherwise undefined behaviour) for a
   * given point
   * @param point to calculate distance to bounding polygon. If not in original Crs set apply transformation to true
   * @param transformToDestinationCrs when true transform point to projection (destination Crs of converter assumed to
   *                        be a projected), when false it is assumed to already be projected and calculated as is
   * @return distance in destination Crs units (typically meters)
   */
  public double calculateProjectedDistanceToBoundingPolygon(Point point, boolean transformToDestinationCrs){
    return indexedBoundingPolygonDistProjected.distance(
        transformToDestinationCrs ?
            PlanitJtsUtils.transformGeometrySafe(point, mathTransformSourceToProjection): point);
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
   * @param geometryInOriginalCrs to check
   * @param isWithinWhenNoBoundary when true, true is returned if provided boundary has no polygon defined,
   *                               false otherwise
   * @return true when within boundary, false otherwise
   */
  public boolean isPartlyOrWhollyWithinBoundaryArea(
      Geometry geometryInOriginalCrs,
      boolean isWithinWhenNoBoundary){
    if(preppedBoundingPolygonOriginalCrs == null){
      return isWithinWhenNoBoundary;
    }

    return preppedBoundingPolygonOriginalCrs.intersects(geometryInOriginalCrs);
  }

  /**
   * Verify if geometry is (partly) within boundary provided.
   *
   * @param pointInOriginalCrs to check
   * @param maxProjectedDistanceToBoundary to allow
   * @param isWithinWhenNoBoundary when true, true is returned if provided boundary has no polygon defined,
   *                               false otherwise
   * @return true when within boundary, false otherwise
   */
  public boolean isNearPartlyOrWhollyWithinBoundaryArea(
      Point pointInOriginalCrs,
      double maxProjectedDistanceToBoundary,
      boolean isWithinWhenNoBoundary){
    if(preppedBoundingPolygonOriginalCrs == null){
      return isWithinWhenNoBoundary;
    }

    boolean success = isPartlyOrWhollyWithinBoundaryArea(pointInOriginalCrs, isWithinWhenNoBoundary);
    if(!success && maxProjectedDistanceToBoundary > 0){
      success = maxProjectedDistanceToBoundary <
          this.calculateProjectedDistanceToBoundingPolygon(pointInOriginalCrs, true);
    }
    return success;
  }

  /**
   * Verify if geometry is (partly) within boundary provided.
   *
   * @param lineStringInOriginalCrs to check
   * @param maxProjectedDistanceToBoundary to allow
   * @param isWithinWhenNoBoundary when true, true is returned if provided boundary has no polygon defined,
   *                               false otherwise
   * @return true when within boundary, false otherwise
   */
  public boolean isNearPartlyOrWhollyWithinBoundaryArea(
      LineString lineStringInOriginalCrs, double maxProjectedDistanceToBoundary, boolean isWithinWhenNoBoundary){

    if(preppedBoundingPolygonOriginalCrs == null){
      return isWithinWhenNoBoundary;
    }

    boolean success = isPartlyOrWhollyWithinBoundaryArea(lineStringInOriginalCrs, isWithinWhenNoBoundary);
    if(!success && maxProjectedDistanceToBoundary > 0){
      for(int index=0; index < lineStringInOriginalCrs.getNumPoints();++index){
        var currPoint = lineStringInOriginalCrs.getPointN(index);
        success = maxProjectedDistanceToBoundary <
            this.calculateProjectedDistanceToBoundingPolygon(currPoint, true);
        if(success){
          break;
        }
      }
    }
    return success;
  }




}
