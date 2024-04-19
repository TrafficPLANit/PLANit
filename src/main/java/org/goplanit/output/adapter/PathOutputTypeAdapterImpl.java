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
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
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
   * Collect output property values that do not depend on path information directly (only indirect such
   * as origin and destination zones encapsulated in the iterator). If no match is found  an empty option is returned
   *
   * @param outputProperty to collect
   * @param odMultiPathIterator to use
   * @return result (if any match)
   * @throws PlanItException thrown if error
   */
  protected Optional<?> getPathIndependentPropertyValue(
          OutputProperty outputProperty,
          OdMultiPathIterator<?,?> odMultiPathIterator) throws PlanItException {

    Optional<?> value = null;
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
        value = Optional.empty();
    }
    return value;
  }

  /**
   * Collect output property values that do depend on path information directly.
   * If no match is found  an empty option is returned
   *
   * @param outputProperty to collect
   * @param path to use
   * @return result (if any match)
   * @throws PlanItException thrown if error
   */
  protected Optional<?> getPathDependentPropertyValue(
          OutputProperty outputProperty,
          PathOutputIdentificationType pathOutputType,
          ManagedDirectedPath path) throws PlanItException {

    switch (outputProperty.getOutputPropertyType()) {
      case PATH_STRING:
        return PathOutputTypeAdapter.getPathAsString(path, pathOutputType);
      case PATH_ID:
        return PathOutputTypeAdapter.getPathId(path);
      default:
        return Optional.of(String.format("Tried to find property of %s which is not applicable for OD path", outputProperty.getName()));
    }
  }

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
  public Optional<? extends List<?>> getPathOutputPropertyValue(
          OutputProperty outputProperty,
          OdMultiPathIterator<?,?> odMultiPathIterator,
          Mode mode,
          TimePeriod timePeriod,
          PathOutputIdentificationType pathOutputType) {
    try {
      var paths = odMultiPathIterator.getCurrentValue();

      // try output type independent results
      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (!value.isPresent()) {
        // try non-path specific results
        value = getPathIndependentPropertyValue(outputProperty, odMultiPathIterator);
      }

      if (value.isPresent()) {
        // repeat #path options times
        return Optional.of(new ArrayList<>(Collections.nCopies(paths.size(), value.get())));
      }

      var valueList = new ArrayList<>(paths.size());
      for(var path : paths){
        // path dependent result, construct on a per-path basis
        valueList.add(getPathDependentPropertyValue(outputProperty, pathOutputType, path).orElse(null));
      }
      return Optional.of(valueList);


      // no unit convertable types here, so do not verify if conversion is needed
    } catch (PlanItException e) {
      return Optional.of(List.of(e.getMessage()));
    }
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
  public Optional<?> getPathOutputPropertyValue(
          OutputProperty outputProperty,
          OdMultiPathIterator<?,?> odMultiPathIterator,
          int multiPathIndex,
          Mode mode,
          TimePeriod timePeriod,
          PathOutputIdentificationType pathOutputType) {
    try {
      var path = odMultiPathIterator.getCurrentValue().get(multiPathIndex);

      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (!value.isPresent()) {
        // try non-path specific results
        value = getPathIndependentPropertyValue(outputProperty, odMultiPathIterator);
      }

      if (value.isPresent()) {
        return value;
      }

      // try path dependent properties
      return getPathDependentPropertyValue(outputProperty, pathOutputType, path);

      // no unit convertable types here, so do not verify if conversion is needed
    } catch (PlanItException e) {
      return Optional.of(List.of(e.getMessage()));
    }
  }

}
