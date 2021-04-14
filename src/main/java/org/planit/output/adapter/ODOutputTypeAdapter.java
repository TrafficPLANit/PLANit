package org.planit.output.adapter;

import java.util.Optional;

import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;

/**
 * Interface defining the methods required for an Origin-Destination output adapter
 * 
 * @author gman6028, markr
 *
 */
public interface ODOutputTypeAdapter extends OutputTypeAdapter {
  
  /**
   * Returns the external Id of the destination zone for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the external Id of the destination zone for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneExternalId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentDestination().getExternalId());
  }
  
  /**
   * Returns the Xml Id of the destination zone for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the Xml Id of the destination zone for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneXmlId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentDestination().getXmlId());
  }  

  /**
   * Returns the Id of the destination zone for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the Id of the destination zone for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getDestinationZoneId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentDestination().getId());
  }

  /**
   * Returns the origin zone external Id for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the origin zone external Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneExternalId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentOrigin().getExternalId());
  }
  
  /**
   * Returns the origin zone Xml Id for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the origin zone Xml Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneXmlId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentOrigin().getXmlId());
  }  

  /**
   * Returns the origin zone Id for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator ODMatrixIterator object containing the required data
   * @return the origin zone Id for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getOriginZoneId(ODMatrixIterator odMatrixIterator) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentOrigin().getId());
  }

  /**
   * Returns the OD travel cost for the current cell in the OD skim matrix
   * 
   * @param odMatrixIterator   ODMatrixIterator object containing the required data
   * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
   * @return the OD travel cost for the current cell in the OD skim matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Double> getODCost(ODMatrixIterator odMatrixIterator, double timeUnitMultiplier) throws PlanItException {
    return Optional.of(odMatrixIterator.getCurrentValue() * timeUnitMultiplier);
  }  

  /**
   * Retrieve an OD skim matrix for a specified OD skim output type and mode
   * 
   * @param odSkimOutputType the specified OD skim output type
   * @param mode the specified mode
   * @return the OD skim matrix
   */
  public abstract Optional<ODSkimMatrix> getODSkimMatrix(ODSkimSubOutputType odSkimOutputType, Mode mode);

  /**
   * Returns the specified output property values for the current cell in the OD Matrix Iterator
   * 
   * @param outputProperty the specified output property
   * @param odMatrixIterator the iterator through the current OD Matrix
   * @param mode the current mode
   * @param timePeriod the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public abstract Optional<?> getODOutputPropertyValue(OutputProperty outputProperty, ODMatrixIterator odMatrixIterator, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier);

}
