package org.planit.network.converter;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.utils.epsg.EpsgCodesByCountry;
import org.planit.utils.exceptions.PlanItException;

/**
 * abstract class implementation to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public abstract class NetworkWriterImpl implements NetworkWriter {

  /**
   * the id mapper to use
   */
  protected IdMapperType idMapper;

  /**
   * identify what the destination Crs is supposed to be. If directly set by user we use the overwrite Crs, if null then we try to extract an appropriate Crs by the country name.
   * If no such mapping exists, we return the fall back option that must be provided.
   * 
   * @param overwriteCrs this Crs takes precedence and is returned if present
   * @param countryName  extract appropriate Crs based on countryname if overwriteCrs is not provided
   * @param fallBackCrs  returned when none of the two other options yielded a result
   * @throws PlanItException thrown if error
   */
  protected CoordinateReferenceSystem identifyDestinationCoordinateReferenceSystem(CoordinateReferenceSystem overwriteCrs, String countryName,
      CoordinateReferenceSystem fallBackCrs) throws PlanItException {

    /* CRS and transformer (if needed) */
    CoordinateReferenceSystem destinationCrs = overwriteCrs;
    if (destinationCrs == null) {
      destinationCrs = PlanitOpenGisUtils.createCoordinateReferenceSystem(EpsgCodesByCountry.getEpsg(countryName));
    }
    if (destinationCrs == null) {
      destinationCrs = fallBackCrs;
    }
    PlanItException.throwIfNull(destinationCrs, "destination Coordinate Reference System is null, this is not allowed");
    return destinationCrs;
  }

  /**
   * constructor
   * 
   * @param idMapper to use as default
   */
  protected NetworkWriterImpl(IdMapperType idMapper) {
    setIdMapper(idMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdMapperType getIdMapper() {
    return idMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdMapper(IdMapperType idMapper) {
    this.idMapper = idMapper;
  }

}
