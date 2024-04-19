package org.goplanit.output.adapter;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.path.OdMultiPathIterator;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
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
   * Returns the specified output property values for the current cell in the ODMulti-PathIterator
   * <p>
   * Since we allow for multiple paths per OD, we may end up with multiple values, one per path. Hence, we process
   * each path separately resulting in a column vector inf the form of a list which x times the same value in case the output property is constant
   * across the paths, or x different results in case it varies per path.
   * </p>
   *
   * @param outputProperty the specified output property
   * @param odMultiPathIterator the iterator through the current ODMulti-Path object
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @param pathOutputType the type of objects in the path list
   * @return the value(s) of the specified property for each path available for the OD
   */
  @Override
  public Optional<? extends List<?>> getPathOutputPropertyValues(
          OutputProperty outputProperty,
          OdMultiPathIterator<?,?> odMultiPathIterator,
          Mode mode,
          TimePeriod timePeriod,
          PathOutputIdentificationType pathOutputType) {
    try {
      var paths = odMultiPathIterator.getCurrentValue();

      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return Optional.of(new ArrayList<>(Collections.nCopies(paths.size(), value.get())));
      }

     // first deal with all non-path specific results
      switch (outputProperty.getOutputPropertyType()) {
        case DESTINATION_ZONE_EXTERNAL_ID:
          value = PathOutputTypeAdapter.getDestinationZoneExternalId(odMultiPathIterator);
          break;
        case DESTINATION_ZONE_XML_ID:
          value = PathOutputTypeAdapter.getDestinationZoneXmlId(odMultiPathIterator);
          break;
        case DESTINATION_ZONE_ID:
          value = PathOutputTypeAdapter.getDestinationZoneId(odMultiPathIterator);
          break;
        case ORIGIN_ZONE_EXTERNAL_ID:
          value = PathOutputTypeAdapter.getOriginZoneExternalId(odMultiPathIterator);
          break;
        case ORIGIN_ZONE_XML_ID:
          value = PathOutputTypeAdapter.getOriginZoneXmlId(odMultiPathIterator);
          break;
        case ORIGIN_ZONE_ID:
          value = PathOutputTypeAdapter.getOriginZoneId(odMultiPathIterator);
          break;
        default:
          // ok, as might be path specific, just ignore
      }
      if (value.isPresent()) {
        return Optional.of(new ArrayList<>(Collections.nCopies(paths.size(), value.get())));
      }

      // path dependent result, construct on a per-path bases
      return Optional.of(paths.stream().map( p -> {
                switch (outputProperty.getOutputPropertyType()) {
                  case PATH_STRING:
                    return PathOutputTypeAdapter.getPathAsString(p, pathOutputType);
                  case PATH_ID:
                    return PathOutputTypeAdapter.getPathId(p);
                  default:
                    return Optional.of(String.format("Tried to find property of %s which is not applicable for OD path", outputProperty.getName()));
                }
              }).collect(Collectors.toList()));


      // no unit convertable types here, so do not verify if conversion is needed
    } catch (PlanItException e) {
      return Optional.of(List.of(e.getMessage()));
    }
  }
}
