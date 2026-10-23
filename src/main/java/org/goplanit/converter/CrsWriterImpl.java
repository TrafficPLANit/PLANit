package org.goplanit.converter;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdMapperType;
import org.locationtech.jts.geom.Coordinate;

import java.util.logging.Logger;

/**
 * Base class for writers that depend on coordinate reference system
 * @param <T> type of writer
 */
public abstract class CrsWriterImpl<T> extends BaseWriterImpl<T>{

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(CrsWriterImpl.class.getCanonicalName());

  /** helper on Crs conversion */
  CrsConversionHelper crsConversionHelper;

  /** transform the coordinate based on the destination transformer
   * @param coordinate to transform
   * @return transformed coordinate
   */
  protected Coordinate createTransformedCoordinate(final Coordinate coordinate) {
    return crsConversionHelper.createTransformedCoordinate(coordinate);
  }

  /** Transform the coordinate based on the destination transformer
   *
   * @param coordinates to transform
   * @return transformed coordinates (if no conversion is required, input is returned
   */
  protected Coordinate[] getTransformedCoordinates(final Coordinate[] coordinates) {
    return crsConversionHelper.getTransformedCoordinates(coordinates);
  }

  /** prepare the Crs transformer (if any) based on the user configuration settings. To be invoked internally
   * by deriving writer just before actual writing starts
   *
   * @param sourceCrs the crs used for the source material of this writer
   * @param userDefinedDestinationCrs the user configured destination Crs (if any)
   * @param destinationCountry the destination country for which we can construct a Crs in case no
   *                           specific destination Crs is provided
   * @param logResolvedDestinationCrs when true, log how the effective destination CRS was resolved
   */
  protected void prepareCoordinateReferenceSystem(
          CoordinateReferenceSystem sourceCrs,
          CoordinateReferenceSystem userDefinedDestinationCrs,
          String destinationCountry,
          boolean logResolvedDestinationCrs){

    this.crsConversionHelper =
        new CrsConversionHelper(sourceCrs, userDefinedDestinationCrs, destinationCountry, logResolvedDestinationCrs);
  }

  /** get the destination crs transformer. Note it might be null and should only be collected after
   * {@link #prepareCoordinateReferenceSystem(CoordinateReferenceSystem, CoordinateReferenceSystem, String, boolean)} has been
   * invoked which determines if and which transformer should be applied
   *
   * @return destination crs transformer
   */
  protected MathTransform getDestinationCrsTransformer() {
    return crsConversionHelper.getToCrsTransformer();
  }

  /** geo util class based on source Crs (if any)
   * @return geoUtils
   */
  protected PlanitJtsCrsUtils getGeoUtils() {
    return crsConversionHelper.getFromCrsGeoUtils();
  }

  /**
   * Access to destination Crs
   * @return crs
   */
  protected CoordinateReferenceSystem getDestinationCoordinateReferenceSystem() {
    return crsConversionHelper.getToCrs();
  }

  /** Constructor
   *
   * @param idMapperType to use
   */
  protected CrsWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }

}
