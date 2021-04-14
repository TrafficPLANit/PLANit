package org.planit.output.adapter;

import java.util.Optional;

import org.planit.od.odpath.ODPathIterator;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.property.OutputProperty;
import org.planit.path.Path;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;

/**
 * Output type adapter interface for paths
 * 
 * @author gman6028
 *
 */
public interface PathOutputTypeAdapter extends OutputTypeAdapter {
  
  /**
   * Returns the external Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the external Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentDestination().getExternalId());
  }
  
  /**
   * Returns the Xml Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the xml Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneXmlId(ODPathIterator odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentDestination().getXmlId());
  }  

  /**
   * Returns the Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long>getDestinationZoneId(ODPathIterator odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentDestination().getId());
  }

  /**
   * Returns the origin zone external Id for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the origin zone external Id for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentOrigin().getExternalId());
  }
  
  /**
   * Returns the Xml Id of the origin zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the xml Id of the origin zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneXmlId(ODPathIterator odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentOrigin().getXmlId());
  }    

  /**
   * Returns the origin zone Id for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the origin zone Id for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getOriginZoneId(ODPathIterator odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentOrigin().getId());
  }

  /**
   * Returns the path as a String of comma-separated Id values
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @param pathOutputType the type of objects being used in the path
   * @return the OD path as a String of comma-separated node external Id values
   */
  public static Optional<String> getPathAsString(ODPathIterator odPathIterator, PathOutputIdentificationType pathOutputType) {
    Path path = odPathIterator.getCurrentValue();
    if (path != null) {
      return Optional.of(path.toString(pathOutputType));
    }
    return Optional.of("");
  }

  /**
   * Return the Id of the current path
   * 
   * If there is no path between the current origin and destination zones, this returns -1
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the id of the current path, or -1 if no path exists
   */
  public static Optional<Long> getPathId(ODPathIterator odPathIterator) {
    Path path = odPathIterator.getCurrentValue();
    if (path == null) {
      return Optional.of(-1l);
    }
    return Optional.of(odPathIterator.getCurrentValue().getId());
  }  

  /**
   * Retrieve an OD path matrix object for a specified mode
   * 
   * @param mode the specified mode
   * @return the OD path object
   */
  public abstract Optional<ODPathMatrix> getODPathMatrix(Mode mode);

  /**
   * Returns the specified output property values for the current cell in the ODPathIterator
   * 
   * @param outputProperty the specified output property
   * @param odPathIterator the iterator through the current ODPath object
   * @param mode the current mode
   * @param timePeriod the current time period
   * @param pathOutputType the type of objects in the path list
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public abstract Optional<?> getPathOutputPropertyValue(
      OutputProperty outputProperty, ODPathIterator odPathIterator, Mode mode, TimePeriod timePeriod, PathOutputIdentificationType pathOutputType);
}
