package org.goplanit.converter;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.goplanit.utils.epsg.EpsgCodesByCountry;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdMapperType;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.util.logging.Logger;

public abstract class CrsWriterImpl<T> extends BaseWriterImpl<T>{

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(CrsWriterImpl.class.getCanonicalName());

  /** geo utils */
  private PlanitJtsCrsUtils geoUtils;

  /** The destination Crs we're using */
  CoordinateReferenceSystem destinationCrs;

  /** when the destination CRS differs from the network CRS all geometries require transforming, for which this transformer will be initialised */
  private MathTransform destinationCrsTransformer = null;


  /** transform the coordinate based on the destination transformer
   * @param coordinate to transform
   * @return transformed coordinate
   */
  protected Coordinate createTransformedCoordinate(final Coordinate coordinate) {
    try {
      if(getDestinationCrsTransformer()!=null) {
        return JTS.transform(coordinate, null, getDestinationCrsTransformer());
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
  protected Coordinate[] getTransformedCoordinates(final Coordinate[] coordinates) {
    Coordinate[] transformedCoordinates = null;
    try {
      if(getDestinationCrsTransformer()!=null) {

        transformedCoordinates = new Coordinate[coordinates.length];
        for(int index = 0; index < coordinates.length ; ++index) {
          transformedCoordinates[index] = JTS.transform(coordinates[index], null, getDestinationCrsTransformer());
        }
      }else {
        transformedCoordinates = coordinates;
      }
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to transform coordinates from %s ",coordinates.toString()));
    }
    return transformedCoordinates;
  }

  /**
   * identify what the destination Crs is supposed to be. If directly set by user we use the overwriteCrs, if null then we try to extract an appropriate Crs by the country name.
   * If no such mapping exists, we return the fallback option that must be provided.
   *
   * @param overwriteCrs this Crs takes precedence and is returned if present
   * @param countryName  extract appropriate Crs based on country name if overwriteCrs is not provided
   * @param fallBackCrs  returned when none of the two other options yielded a result
   * @return crs for destination
   */
  private CoordinateReferenceSystem identifyDestinationCoordinateReferenceSystem(
          CoordinateReferenceSystem overwriteCrs, String countryName, CoordinateReferenceSystem fallBackCrs){

    /* CRS and transformer (if needed) */
    CoordinateReferenceSystem destinationCrs = overwriteCrs;
    if (destinationCrs == null && countryName != null) {
      destinationCrs = PlanitCrsUtils.createCoordinateReferenceSystem(EpsgCodesByCountry.getEpsg(countryName));
    }
    if (destinationCrs == null) {
      destinationCrs = fallBackCrs;
    }
    PlanItRunTimeException.throwIfNull(destinationCrs, "Destination Coordinate Reference System is null, this is not allowed");
    return destinationCrs;
  }

  private void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCrs) {
    this.destinationCrs = destinationCrs;
  }

  protected CoordinateReferenceSystem getDestinationCoordinateReferenceSystem(){
    return this.destinationCrs;
  }

  /** Extract the srs name to use based on the available crs information on network and this writer
   *
   * @param crs to use
   * @return srsName to use
   */
  protected static String extractSrsName(CoordinateReferenceSystem crs) {
    String srsName = "";
    if("EPSG".equals(crs.getName().getCodeSpace())) {
      /* spatial crs based on epsg code*/
      Integer epsgCode = null;
      try {
        epsgCode = CRS.lookupEpsgCode(crs, false);
        if(epsgCode == null) {
          /* full scan */
          epsgCode = CRS.lookupEpsgCode(crs, true);
        }
        srsName = String.format("EPSG:%s",epsgCode.toString());
      }catch (Exception e) {
        LOGGER.severe(e.getMessage());
        throw new PlanItRunTimeException("Unable to extract epsg code from destination crs %s", crs.getName());
      }
    }else if(!crs.equals(PlanitJtsCrsUtils.CARTESIANCRS)) {
      throw new PlanItRunTimeException("Unable to extract epsg code from destination crs %s", crs.getName());
    }
    return srsName;
  }

  /** prepare the Crs transformer (if any) based on the user configuration settings. To be invoked internally by deriving writer
   * just before actual writing starts
   *
   * @param sourceCrs the crs used for the source material of this writer
   * @param userDefinedDestinationCrs the user configured destination Crs (if any)
   * @param destinationCountry the destination country for which we can construct a Crs in case no specific destination Crs is provided
   */
  protected void prepareCoordinateReferenceSystem(
          CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem userDefinedDestinationCrs, String destinationCountry){

    PlanItRunTimeException.throwIfNull(sourceCrs, "Source Crs null, this is not allowed");
    this.geoUtils = new PlanitJtsCrsUtils(sourceCrs);

    /* CRS and transformer (if needed) */
    CoordinateReferenceSystem identifiedDestinationCrs =
            identifyDestinationCoordinateReferenceSystem(userDefinedDestinationCrs, destinationCountry, sourceCrs);
    PlanItRunTimeException.throwIfNull(identifiedDestinationCrs, "Destination Coordinate Reference System is null, this is not allowed");

    if(identifiedDestinationCrs != null){
      LOGGER.info(String.format("CRS set to: %s", identifiedDestinationCrs.getName()));
    }

    /* configure crs transformer if required, to be able to convert geometries to preferred CRS while writing */
    if(!identifiedDestinationCrs.equals(sourceCrs)) {
      destinationCrsTransformer = PlanitJtsUtils.findMathTransform(sourceCrs, identifiedDestinationCrs);
      LOGGER.info(String.format("Geometries will be converted from source CRS (%s) to destination CRS (%s) during writing", sourceCrs.getName(), identifiedDestinationCrs.getName()));
    }else{
      LOGGER.info("Source CRS same as destination CRS, no transformation applied during writing");
    }

    setDestinationCoordinateReferenceSystem(identifiedDestinationCrs);
  }

  /** get the destination crs transformer. Note it might be null and should only be collected after
   * {@link #prepareCoordinateReferenceSystem(CoordinateReferenceSystem, CoordinateReferenceSystem, String)} has been
   * invoked which determines if and which transformer should be applied
   *
   * @return destination crs transformer
   */
  protected MathTransform getDestinationCrsTransformer() {
    return destinationCrsTransformer;
  }

  /** geo util class based on source Crs (if any)
   * @return geoUtils
   */
  protected PlanitJtsCrsUtils getGeoUtils() {
    return geoUtils;
  }

  /** Constructor
   *
   * @param idMapperType to use
   */
  protected CrsWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }

}
