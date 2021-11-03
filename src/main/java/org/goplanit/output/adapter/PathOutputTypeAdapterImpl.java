package org.goplanit.output.adapter;

import java.util.Optional;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.path.DirectedPath;
import org.goplanit.utils.time.TimePeriod;

/**
 * Top-level abstract class which defines the common methods required by Path output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class PathOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements PathOutputTypeAdapter {

  /**
   * Constructor
   * 
   * @param outputType        the output type for the current persistence
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
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @param pathOutputType the type of objects in the path list
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  @Override
  public Optional<?> getPathOutputPropertyValue(OutputProperty outputProperty, OdDataIterator<? extends DirectedPath> odPathIterator, Mode mode, TimePeriod timePeriod,
      PathOutputIdentificationType pathOutputType) {
    try {

      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      switch (outputProperty.getOutputPropertyType()) {
      case DESTINATION_ZONE_EXTERNAL_ID:
        return PathOutputTypeAdapter.getDestinationZoneExternalId(odPathIterator);
      case DESTINATION_ZONE_XML_ID:
        return PathOutputTypeAdapter.getDestinationZoneXmlId(odPathIterator);
      case DESTINATION_ZONE_ID:
        return PathOutputTypeAdapter.getDestinationZoneId(odPathIterator);
      case PATH_STRING:
        return PathOutputTypeAdapter.getPathAsString(odPathIterator, pathOutputType);
      case PATH_ID:
        return PathOutputTypeAdapter.getPathId(odPathIterator);
      case ORIGIN_ZONE_EXTERNAL_ID:
        return PathOutputTypeAdapter.getOriginZoneExternalId(odPathIterator);
      case ORIGIN_ZONE_XML_ID:
        return PathOutputTypeAdapter.getOriginZoneXmlId(odPathIterator);
      case ORIGIN_ZONE_ID:
        return PathOutputTypeAdapter.getOriginZoneId(odPathIterator);
      default:
        return Optional.of(String.format("Tried to find link property of %s which is not applicable for OD path", outputProperty.getName()));
      }

      // no unit convertable types here, so do not verify if conversion is needed
    } catch (PlanItException e) {
      return Optional.of(e.getMessage());
    }
  }
}
