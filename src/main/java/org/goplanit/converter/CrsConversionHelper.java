package org.goplanit.converter;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.goplanit.utils.epsg.ProjectedEpsgCodesByCountry;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.Coordinate;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Helper class encapsulating a CRs and transformation utils for conversions
 */
public final class CrsConversionHelper{

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(CrsConversionHelper.class.getCanonicalName());

  /** geo utils */
  private PlanitJtsCrsUtils geoUtils;

  /** The original Crs */
  CoordinateReferenceSystem fromSrs;

  /** The Crs we're looking to end up with */
  CoordinateReferenceSystem toCrs;

  /** transformer */
  private MathTransform toCrsTransformer = null;

  /**
   * identify what the destination Crs is supposed to be. If directly set by user we use the overwriteCrs,
   * if null then we try to extract an appropriate Crs by the country name.
   * If no such mapping exists, we return the fallback option that must be provided.
   *
   * @param overwriteCrs this Crs takes precedence and is returned if present
   * @param countryName  extract appropriate Crs based on country name if overwriteCrs is not provided
   * @param fallBackCrs  returned when none of the two other options yielded a result
   * @return crs for destination
   */
  private Pair<CoordinateReferenceSystem, String> identifyDestinationCoordinateReferenceSystem(
      CoordinateReferenceSystem overwriteCrs, String countryName, CoordinateReferenceSystem fallBackCrs){

    /* CRS and transformer (if needed) based on dedicated non-generic country*/
    CoordinateReferenceSystem destinationCrs = overwriteCrs;
    String crsSource = "configured in settings";
    if (destinationCrs == null && countryName != null && !countryName.equals(CountryNames.GLOBAL)) {
      destinationCrs = PlanitCrsUtils.createCoordinateReferenceSystem(ProjectedEpsgCodesByCountry.getEpsg(countryName));
      if(destinationCrs != null) {
        crsSource = String.format("derived from country %s", countryName);
      }
    }
    // if not found, we prefer the fallback CRS if available
    if (destinationCrs == null) {
      destinationCrs = fallBackCrs;
      crsSource = "derived from source CRS";
    }

    PlanItRunTimeException.throwIfNull(destinationCrs, "Destination Coordinate Reference System is null, " +
        "this is not allowed");
    return Pair.of(destinationCrs, crsSource);
  }

  private void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCrs) {
    this.toCrs = destinationCrs;
  }

  /** prepare the Crs transformer (if any).
   *
   * @param fromCrs the crs used for the source
   * @param toCrs the user configured destination Crs (if any)
   * @param toCountry the destination country for which we can construct a Crs in case no
   *                           specific destination Crs is provided
   * @param logResolvedToCrs when true, log how the effective destination CRS was resolved
   */
  public CrsConversionHelper(
          CoordinateReferenceSystem fromCrs,
          CoordinateReferenceSystem toCrs,
          String toCountry,
          boolean logResolvedToCrs){

    PlanItRunTimeException.throwIfNull(fromCrs, "Source Crs null, this is not allowed");
    this.geoUtils = new PlanitJtsCrsUtils(fromCrs);

    /* CRS and transformer (if needed) */
    var identifiedDestinationCrsResult =
            identifyDestinationCoordinateReferenceSystem(toCrs, toCountry, fromCrs);
    CoordinateReferenceSystem identifiedDestinationCrs = identifiedDestinationCrsResult.first();
    String identifiedDestinationCrsSource = identifiedDestinationCrsResult.second();
    PlanItRunTimeException.throwIfNull(identifiedDestinationCrs,
        "Destination Coordinate Reference System is null, this is not allowed");

    if(logResolvedToCrs) {
      if("configured in settings".equals(identifiedDestinationCrsSource)) {
        LOGGER.info(String.format("CRS set to: %s", identifiedDestinationCrs.getName()));
      } else {
        LOGGER.info(String.format("CRS set to: %s (%s)",
            identifiedDestinationCrs.getName(), identifiedDestinationCrsSource));
      }
    }

    /* configure crs transformer if required, to be able to convert geometries to preferred CRS while writing */
    if(!identifiedDestinationCrs.equals(fromCrs)) {
      toCrsTransformer = PlanitJtsUtils.findMathTransform(fromCrs, identifiedDestinationCrs);
      if(logResolvedToCrs) {
        LOGGER.info(String.format("Geometries will be converted from source CRS (%s) to destination CRS (%s)" +
            " during writing", fromCrs.getName(), identifiedDestinationCrs.getName()));
      }
    }else{
      if(logResolvedToCrs) {
        LOGGER.info("Source CRS same as destination CRS, no transformation applied during writing");
      }
    }

    setDestinationCoordinateReferenceSystem(identifiedDestinationCrs);
  }

  /** get the destination crs transformer. Note it might be null
   *
   * @return destination crs transformer
   */
  public MathTransform getToCrsTransformer() {
    return toCrsTransformer;
  }

  /** geo util class based on source Crs (if any)
   * @return geoUtils
   */
  public PlanitJtsCrsUtils getFromCrsGeoUtils() {
    return geoUtils;
  }

  /** transform the coordinate based on the destination transformer
   * @param coordinate to transform
   * @return transformed coordinate
   */
  public Coordinate createTransformedCoordinate(final Coordinate coordinate) {
    try {
      if(getToCrsTransformer()!=null) {
        return JTS.transform(coordinate, null, getToCrsTransformer());
      }
      return coordinate;
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to transform coordinate from %s ",coordinate.toString()));
    }
    return null;
  }

  /** Transform the coordinate based on the destination transformer
   *
   * @param coordinates to transform
   * @return transformed coordinates (if no conversion is required, input is returned
   */
  public Coordinate[] getTransformedCoordinates(final Coordinate[] coordinates) {
    Coordinate[] transformedCoordinates = null;
    try {
      if(getToCrsTransformer()!=null) {

        transformedCoordinates = new Coordinate[coordinates.length];
        for(int index = 0; index < coordinates.length ; ++index) {
          transformedCoordinates[index] = JTS.transform(coordinates[index], null, getToCrsTransformer());
        }
      }else {
        transformedCoordinates = coordinates;
      }
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to transform coordinates from %s ", Arrays.toString(coordinates)));
    }
    return transformedCoordinates;
  }

  /**
   * access to CRS
   * @return crs
   */
  public CoordinateReferenceSystem getToCrs(){
    return this.toCrs;
  }

}
