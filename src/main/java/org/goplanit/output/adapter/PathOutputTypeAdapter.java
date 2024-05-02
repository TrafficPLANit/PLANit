package org.goplanit.output.adapter;

import java.util.List;
import java.util.Optional;

import org.goplanit.od.path.OdMultiPathIterator;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.PathUtils;
import org.goplanit.utils.time.TimePeriod;
import org.locationtech.jts.geom.LineString;

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
  public static Optional<String> getDestinationZoneExternalId(OdDataIterator<?> odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentDestination().getExternalId());
  }

  /**
   * Returns the Xml Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the xml Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getDestinationZoneXmlId(OdDataIterator<?> odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentDestination().getXmlId());
  }

  /**
   * Returns the Id of the destination zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the Id of the destination zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getDestinationZoneId(OdDataIterator<?> odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentDestination().getId());
  }

  /**
   * Returns the origin zone external Id for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the origin zone external Id for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneExternalId(OdDataIterator<?> odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentOrigin().getExternalId());
  }

  /**
   * Returns the Xml Id of the origin zone for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the xml Id of the origin zone for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<String> getOriginZoneXmlId(OdDataIterator<?> odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentOrigin().getXmlId());
  }

  /**
   * Returns the origin zone Id for the current cell in the OD path matrix
   * 
   * @param odPathIterator ODPathIterator object containing the required data
   * @return the origin zone Id for the current cell in the OD path matrix
   * @throws PlanItException thrown if there is an error
   */
  public static Optional<Long> getOriginZoneId(OdDataIterator<?> odPathIterator) throws PlanItException {
    return Optional.of(odPathIterator.getCurrentOrigin().getId());
  }

  /**
   * Returns the path as a String of comma-separated Id values
   * 
   * @param path path object containing the required data
   * @param pathOutputType the type of objects being used in the path
   * @return the OD path as a String of comma-separated node external Id values
   */
  public static Optional<String> getPathAsString(ManagedDirectedPath path, PathOutputIdentificationType pathOutputType) {
    if (path != null) {
      switch (pathOutputType) {
      case LINK_SEGMENT_EXTERNAL_ID:
        return Optional.of(PathUtils.getEdgeSegmentPathString(path, EdgeSegment::getExternalId));
      case LINK_SEGMENT_XML_ID:
        return Optional.of(PathUtils.getEdgeSegmentPathString(path, EdgeSegment::getXmlId));
      case LINK_SEGMENT_ID:
        return Optional.of(PathUtils.getEdgeSegmentPathString(path, EdgeSegment::getId));
      case NODE_EXTERNAL_ID:
        return Optional.of(PathUtils.getNodePathString(path, Vertex::getExternalId));
      case NODE_XML_ID:
        return Optional.of(PathUtils.getNodePathString(path, Vertex::getXmlId));
      case NODE_ID:
        return Optional.of(PathUtils.getNodePathString(path, Vertex::getId));
      default:
        return Optional.of("");
      }
    }
    return Optional.of("");
  }

  /**
   * Return the Id of the current path
   * 
   * If there is no path between the current origin and destination zones, this returns -1
   * 
   * @param path Path object containing the required data
   * @return the id of the current path, or -1 if no path exists
   */
  public static Optional<Long> getPathId(ManagedDirectedPath path) {
    if (path == null) {
      return Optional.of(-1l);
    }
    return Optional.of(path.getId());
  }

  /**
   * Return the geometry of the path in the form of a path string
   *
   * @param path to extract geometry from
   * @return path geometry
   */
  static Optional<?> getPathGeometry(ManagedDirectedPath path) {
    if(path == null){
      return Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    var geometry = path.createGeometry();
    return geometry != null ? Optional.of( geometry) : Optional.of(PROPERTY_NOT_AVAILABLE);
  }

  /**
   * Retrieve an OD path matrix object for a specified mode
   * 
   * @param mode the specified mode
   * @return the OD path object
   */
  //public abstract Optional<OdPathMatrix> getOdPathMatrix(Mode mode);

  /**
   * Retrieve OD paths for a specified mode. Each OD may have one or more paths
   *
   * @param mode the specified mode
   * @return the OD (multi-)paths object
   */
  public abstract Optional<OdMultiPaths<?,?>> getOdMultiPaths(Mode mode);

  /**
   * Returns the specified output property values for the current cell in the OdMultiPathIterator.
   * this version will produce x results for the property based on how many paths there are present
   * for the given od, e.g., if 5 paths exist for the od, and an originId is the output property then
   * 5 times the same origin id is produced, if the path string is required, then the 5 unique path strings are
   * constructed and returned
   * 
   * @param outputProperty the specified output property
   * @param odMultiPathIterator the iterator through the current ODMultiPath object
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @param pathOutputType the type of objects in the path list
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public abstract Optional<? extends List<?>> getPathOutputPropertyValue(
          OutputProperty outputProperty,
          OdMultiPathIterator<?,?> odMultiPathIterator,
          Mode mode,
          TimePeriod timePeriod,
          PathOutputIdentificationType pathOutputType);

  /**
   * Returns the specified output property values for the current cell in the OdMultiPathIterator. This version is
   * specifically tied to a particular path for the od, e.g., if the od has 5 paths, then an index between 0 and 4
   * is expected to obtain just the result relevant for that path.
   *
   * @param outputProperty the specified output property
   * @param odMultiPathIterator the iterator through the current ODMultiPath object
   * @param multiPathIndex the od path to base this on given multiple entries exist on the current iterator point
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @param pathOutputType the type of objects in the path list
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public abstract Optional<?> getPathOutputPropertyValue(
          OutputProperty outputProperty,
          OdMultiPathIterator<?,?> odMultiPathIterator,
          int multiPathIndex,
          Mode mode,
          TimePeriod timePeriod,
          PathOutputIdentificationType pathOutputType);
}
