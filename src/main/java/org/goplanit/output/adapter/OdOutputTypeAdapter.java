package org.goplanit.output.adapter;

import java.util.Optional;

import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.zonetozone.ZoneToZoneDataIterator;
import org.goplanit.utils.time.TimePeriod;

/**
 * Interface defining the methods required for an Origin-Destination output adapter
 * 
 * @author gman6028, markr
 *
 */
public interface OdOutputTypeAdapter extends OutputTypeAdapter {

  /**
   * Returns the external Id of the destination zone for the current OD
   * 
   * @param odIterator OdIterator object containing the required data
   * @return the external Id of the destination zone for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneExternalId(final ZoneToZoneDataIterator<?> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentToZone().getExternalId());
  }

  /**
   * Returns the XML Id of the destination zone for the current OD destination
   * 
   * @param odIterator OdIterator object containing the required data
   * @return the XML Id of the destination zone for the current OD destination
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneXmlId(final ZoneToZoneDataIterator<?> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentToZone().getXmlId());
  }

  /**
   * Returns the Id of the destination zone for the current cell in the OD destination
   * 
   * @param odIterator OdIterator object containing the required data
   * @return the Id of the destination zone for the current cell OD destination
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getDestinationZoneId(final ZoneToZoneDataIterator<?> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentToZone().getId());
  }

  /**
   * Returns the origin zone external Id for the current cell in the OD origin
   * 
   * @param odIterator OdIterator object containing the required data
   * @return the origin zone external Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneExternalId(final ZoneToZoneDataIterator<?> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentFromZone().getExternalId());
  }

  /**
   * Returns the origin zone XML Id for the current cell in the OD origin
   * 
   * @param odIterator OdIterator object containing the required data
   * @return the origin zone XML Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneXmlId(final ZoneToZoneDataIterator<?> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentFromZone().getXmlId());
  }

  /**
   * Returns the origin zone Id for the current cell in the OD origin
   * 
   * @param odIterator OdIterator object containing the required data
   * @return the origin zone Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getOriginZoneId(final ZoneToZoneDataIterator<?> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentFromZone().getId());
  }

  /**
   * Returns the Od value
   * 
   * @param <T>        type of the return value to expect
   * @param odIterator OdIterator object containing the current value
   * @return the OD travel cost for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static <T> Optional<T> getOdValue(final ZoneToZoneDataIterator<T> odIterator)
      throws PlanItException {
    return Optional.of(odIterator.getCurrentValue());
  }

  /**
   * Retrieve an OD skim matrix for a specified OD skim output type and mode
   * 
   * @param odSkimOutputType the specified OD skim output type
   * @param mode             the specified mode
   * @return the OD skim matrix
   */
  public abstract Optional<OdSkimMatrix> getOdSkimMatrix(SkimSubOutputType odSkimOutputType, Mode mode);

  /**
   * Returns the specified output property values for the current cell in the OD Matrix Iterator
   * 
   * @param outputProperty the specified output property
   * @param odIterator     the iterator through the current OD data
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public abstract Optional<?> getOdOutputPropertyValue(
      OutputProperty outputProperty, final ZoneToZoneDataIterator<?> odIterator, Mode mode, TimePeriod timePeriod);

}
