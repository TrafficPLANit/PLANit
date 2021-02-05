package org.planit.converter;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.utils.epsg.EpsgCodesByCountry;
import org.planit.utils.exceptions.PlanItException;

/**
 * abstract base class implementation to write a PLANit network to disk with id mapping sorted for convenience
 * 
 * @author markr
 *
 */
public abstract class BaseWriterImpl<T> implements ConverterWriter<T> {

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
   * @return crs for destination
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
   * @param idMapperType to use as default
   */
  protected BaseWriterImpl(IdMapperType idMapperType) {
    setIdMapperType(idMapperType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdMapperType getIdMapperType() {
    return idMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdMapperType(IdMapperType idMapper) {
    this.idMapper = idMapper;
  }
   

}
