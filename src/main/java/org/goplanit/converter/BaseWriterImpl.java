package org.goplanit.converter;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.PlanitComponentIdMapper;
import org.goplanit.converter.idmapping.PlanitComponentIdMappers;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.utils.epsg.EpsgCodesByCountry;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitCrsUtils;

/**
 * abstract base class implementation to write a PLANit network to disk with id mapping sorted for convenience
 * 
 * @author markr
 *
 */
public abstract class BaseWriterImpl<T> implements ConverterWriter<T> {

  /**
   * the (primary) id mapper to use
   */
  protected IdMapperType idMapper;

  /** id mappers for components entities */
  private PlanitComponentIdMappers componentIdMappers = new PlanitComponentIdMappers();

  /**
   * Access to the id mappers for each component via dedicated class
   *
   * @return component id mappers container class
   */
  protected PlanitComponentIdMappers getComponentIdMappers(){
    return componentIdMappers;
  }

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
    if (destinationCrs == null && countryName != null) {
      destinationCrs = PlanitCrsUtils.createCoordinateReferenceSystem(EpsgCodesByCountry.getEpsg(countryName));
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

  /**
   * The (main) Id mapper used by this writer (only present after write has been completed)
   *
   * @return mapper
   */
  public abstract PlanitComponentIdMapper getPrimaryIdMapper();

  /**
   * The explicit id mapping to be used by the parent(s), so we use the appropriate referencing. These are to be
   * set before writing commences, otherwise defaults based on the(primary) idMapperType will be instantiated and
   * used instead.
   *
   * @param parentMappers to register
   */
  public void setParentIdMappers(PlanitComponentIdMapper... parentMappers) {
    for(var mapper : parentMappers) {
      componentIdMappers.setDedicatedIdMapper(mapper);
    }
  }

}
