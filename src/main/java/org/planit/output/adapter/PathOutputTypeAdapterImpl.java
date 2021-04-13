package org.planit.output.adapter;

import org.planit.assignment.TrafficAssignment;
import org.planit.od.odpath.ODPathIterator;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.path.Path;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;

/**
 * Top-level abstract class which defines the common methods required by Path output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class PathOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements PathOutputTypeAdapter {

  /**
   * Returns the external Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the external Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  protected String getDestinationZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
    return odPathIterator.getCurrentDestination().getExternalId();
  }
  
  /**
   * Returns the Xml Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the xml Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  protected String getDestinationZoneXmlId(ODPathIterator odPathIterator) throws PlanItException {
    return odPathIterator.getCurrentDestination().getXmlId();
  }  

  /**
   * Returns the Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  protected long getDestinationZoneId(ODPathIterator odPathIterator) throws PlanItException {
    return odPathIterator.getCurrentDestination().getId();
  }

  /**
   * Returns the origin zone external Id for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the origin zone external Id for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  protected Object getOriginZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
    return odPathIterator.getCurrentOrigin().getExternalId();
  }
  
  /**
   * Returns the Xml Id of the origin zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the xml Id of the origin zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  protected String getOriginZoneXmlId(ODPathIterator odPathIterator) throws PlanItException {
    return odPathIterator.getCurrentOrigin().getXmlId();
  }    

  /**
   * Returns the origin zone Id for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the origin zone Id for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  protected long getOriginZoneId(ODPathIterator odPathIterator) throws PlanItException {
    return odPathIterator.getCurrentOrigin().getId();
  }

  /**
   * Returns the path as a String of comma-separated Id values
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @param pathOutputType the type of objects being used in the path
   * @return the OD path as a String of comma-separated node external Id values
   */
  protected String getPathAsString(ODPathIterator odPathIterator, PathOutputIdentificationType pathOutputType) {
    Path path = odPathIterator.getCurrentValue();
    if (path != null) {
      return path.toString(pathOutputType);
    }
    return "";
  }

  /**
   * Return the Id of the current path
   * 
   * If there is no path between the current origin and destination zones, this returns -1
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the id of the current path, or -1 if no path exists
   */
  protected long getPathId(ODPathIterator odPathIterator) {
    Path path = odPathIterator.getCurrentValue();
    if (path == null) {
      return -1;
    }
    return odPathIterator.getCurrentValue().getId();
  }

  /**
   * Constructor
   * 
   * @param outputType the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public PathOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

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
  @Override
  public Object getPathOutputPropertyValue(OutputProperty outputProperty, ODPathIterator odPathIterator, Mode mode,
      TimePeriod timePeriod, PathOutputIdentificationType pathOutputType) {
    try {
      Object obj = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (obj != null) {
        return obj;
      }
      switch (outputProperty) {
        case DESTINATION_ZONE_EXTERNAL_ID:
          return getDestinationZoneExternalId(odPathIterator);
        case DESTINATION_ZONE_XML_ID:
          return getDestinationZoneXmlId(odPathIterator);          
        case DESTINATION_ZONE_ID:
          return getDestinationZoneId(odPathIterator);
        case PATH_STRING:
          return getPathAsString(odPathIterator, pathOutputType);
        case PATH_ID:
          return getPathId(odPathIterator);
        case ORIGIN_ZONE_EXTERNAL_ID:
          return getOriginZoneExternalId(odPathIterator);
        case ORIGIN_ZONE_XML_ID:
          return getOriginZoneXmlId(odPathIterator);          
        case ORIGIN_ZONE_ID:
          return getOriginZoneId(odPathIterator);
        default:
          return new PlanItException("Tried to find link property of " + BaseOutputProperty.convertToBaseOutputProperty(
              outputProperty).getName() + " which is not applicable for OD path");
      }
    } catch (PlanItException e) {
      return e;
    }
  }
}
