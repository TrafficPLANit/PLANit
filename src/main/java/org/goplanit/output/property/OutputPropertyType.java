package org.goplanit.output.property;

import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * Enumeration of possible output properties
 * 
 * @author gman6028
 *
 */
public enum OutputPropertyType {

  // @formatter:off
  /** Output property for density values. */
  DENSITY("org.goplanit.output.property.DensityOutputProperty"),
  /** Output property for internal link ids. */
  LINK_ID("org.goplanit.output.property.LinkIdOutputProperty"),
  /** Output property for link XML ids. */
  LINK_XML_ID("org.goplanit.output.property.LinkXmlIdOutputProperty"),
  /** Output property for link external ids. */
  LINK_EXTERNAL_ID("org.goplanit.output.property.LinkExternalIdOutputProperty"),
  /** Output property for internal link segment ids. */
  LINK_SEGMENT_ID("org.goplanit.output.property.LinkSegmentIdOutputProperty"),
  /** Output property for link segment XML ids. */
  LINK_SEGMENT_XML_ID("org.goplanit.output.property.LinkSegmentXmlIdOutputProperty"), 
  /** Output property for link segment external ids. */
  LINK_SEGMENT_EXTERNAL_ID("org.goplanit.output.property.LinkSegmentExternalIdOutputProperty"),
  /** Output property for link segment geometries. */
  LINK_SEGMENT_GEOMETRY("org.goplanit.output.property.LinkSegmentGeometryOutputProperty"),
  /** Output property for internal mode ids. */
  MODE_ID("org.goplanit.output.property.ModeIdOutputProperty"), 
  /** Output property for mode external ids. */
  MODE_EXTERNAL_ID("org.goplanit.output.property.ModeExternalIdOutputProperty"),
  /** Output property for mode XML ids. */
  MODE_XML_ID("org.goplanit.output.property.ModeXmlIdOutputProperty"), 
  /** Output property for maximum density values. */
  MAXIMUM_DENSITY("org.goplanit.output.property.MaximumDensityOutputProperty"),
  /** Output property for maximum speed values. */
  MAXIMUM_SPEED("org.goplanit.output.property.MaximumSpeedOutputProperty"), 
  /** Output property for calculated speed values. */
  CALCULATED_SPEED("org.goplanit.output.property.CalculatedSpeedOutputProperty"),
  /** Output property for flow values. */
  FLOW("org.goplanit.output.property.FlowOutputProperty"), 
  /** Output property for inflow values. */
  INFLOW("org.goplanit.output.property.InflowOutputProperty"), 
  /** Output property for outflow values. */
  OUTFLOW("org.goplanit.output.property.OutflowOutputProperty"),
  /** Output property for length values. */
  LENGTH("org.goplanit.output.property.LengthOutputProperty"), 
  /** Output property for upstream node internal ids. */
  UPSTREAM_NODE_ID("org.goplanit.output.property.UpstreamNodeIdOutputProperty"),
  /** Output property for upstream node external ids. */
  UPSTREAM_NODE_EXTERNAL_ID("org.goplanit.output.property.UpstreamNodeExternalIdOutputProperty"),
  /** Output property for upstream node geometries. */
  UPSTREAM_NODE_GEOMETRY("org.goplanit.output.property.UpstreamNodeGeometryOutputProperty"),
  /** Output property for upstream node XML ids. */
  UPSTREAM_NODE_XML_ID("org.goplanit.output.property.UpstreamNodeXmlIdOutputProperty"),
  /** Output property for downstream node internal ids. */
  DOWNSTREAM_NODE_ID("org.goplanit.output.property.DownstreamNodeIdOutputProperty"), 
  /** Output property for downstream node external ids. */
  DOWNSTREAM_NODE_EXTERNAL_ID("org.goplanit.output.property.DownstreamNodeExternalIdOutputProperty"),
  /** Output property for downstream node geometries. */
  DOWNSTREAM_NODE_GEOMETRY("org.goplanit.output.property.DownstreamNodeGeometryOutputProperty"),
  /** Output property for downstream node XML ids. */
  DOWNSTREAM_NODE_XML_ID("org.goplanit.output.property.DownstreamNodeXmlIdOutputProperty"), 
  /** Output property for capacity-per-lane values. */
  CAPACITY_PER_LANE("org.goplanit.output.property.CapacityPerLaneOutputProperty"),
  /** Output property for lane-count values. */
  NUMBER_OF_LANES("org.goplanit.output.property.NumberOfLanesOutputProperty"), 
  /** Output property for link segment cost values. */
  LINK_SEGMENT_COST("org.goplanit.output.property.LinkSegmentCostOutputProperty"),
  /** Output property for origin-destination cost values. */
  OD_COST("org.goplanit.output.property.OdCostOutputProperty"), 
  /** Output property for assignment iteration indices. */
  ITERATION_INDEX("org.goplanit.output.property.IterationIndexOutputProperty"),
  /** Output property for origin zone external ids. */
  ORIGIN_ZONE_EXTERNAL_ID("org.goplanit.output.property.OriginZoneExternalIdOutputProperty"),
  /** Output property for origin zone geometries. */
  ORIGIN_ZONE_GEOMETRY("org.goplanit.output.property.OriginZoneGeometryOutputProperty"),
  /** Output property for origin zone internal ids. */
  ORIGIN_ZONE_ID("org.goplanit.output.property.OriginZoneIdOutputProperty"),
  /** Output property for origin zone XML ids. */
  ORIGIN_ZONE_XML_ID("org.goplanit.output.property.OriginZoneXmlIdOutputProperty"),
  /** Output property for destination zone external ids. */
  DESTINATION_ZONE_EXTERNAL_ID("org.goplanit.output.property.DestinationZoneExternalIdOutputProperty"),
  /** Output property for destination zone geometries. */
  DESTINATION_ZONE_GEOMETRY("org.goplanit.output.property.DestinationZoneGeometryOutputProperty"),
  /** Output property for destination zone internal ids. */
  DESTINATION_ZONE_ID("org.goplanit.output.property.DestinationZoneIdOutputProperty"),
  /** Output property for destination zone XML ids. */
  DESTINATION_ZONE_XML_ID("org.goplanit.output.property.DestinationZoneXmlIdOutputProperty"),
  /** Output property for time period internal ids. */
  TIME_PERIOD_ID("org.goplanit.output.property.TimePeriodIdOutputProperty"),
  /** Output property for time period XML ids. */
  TIME_PERIOD_XML_ID("org.goplanit.output.property.TimePeriodXmlIdOutputProperty"), 
  /** Output property for time period external ids. */
  TIME_PERIOD_EXTERNAL_ID("org.goplanit.output.property.TimePeriodExternalIdOutputProperty"),
  /** Output property for route choice convergence gap values. */
  ROUTE_CHOICE_CONVERGENCE_GAP("org.goplanit.output.property.RouteChoiceConvergenceGapOutputProperty"),
  /** Output property for route choice iteration run times. */
  ROUTE_CHOICE_ITERATION_RUN_TIME("org.goplanit.output.property.RouteChoiceIterationRunTimeOutputProperty"),
  /** Output property for run ids. */
  RUN_ID("org.goplanit.output.property.RunIdOutputProperty"),
  /** Output property for path geometries. */
  PATH_GEOMETRY("org.goplanit.output.property.PathGeometryOutputProperty"),
  /** Output property for path ids. */
  PATH_ID("org.goplanit.output.property.PathIdOutputProperty"),
  /** Output property for path counts. */
  PATH_COUNT("org.goplanit.output.property.PathCountOutputProperty"),
  /** Output property for the number of paths added. */
  PATHS_ADDED("org.goplanit.output.property.PathsAddedOutputProperty"),
  /** Output property for the number of paths removed. */
  PATHS_REMOVED("org.goplanit.output.property.PathsRemovedOutputProperty"),
  /** Output property for stringified path representations. */
  PATH_STRING("org.goplanit.output.property.PathOutputStringProperty"),
  /** Output property for volume-capacity ratios. */
  VC_RATIO("org.goplanit.output.property.VCRatioOutputProperty"),
  /** Output property for cost multiplied by flow. */
  COST_TIMES_FLOW("org.goplanit.output.property.CostTimesFlowOutputProperty"), 
  /** Output property for link segment type internal ids. */
  LINK_SEGMENT_TYPE_ID("org.goplanit.output.property.LinkSegmentTypeIdOutputProperty"),
  /** Output property for link segment type names. */
  LINK_SEGMENT_TYPE_NAME("org.goplanit.output.property.LinkSegmentTypeNameOutputProperty"), 
  /** Output property for link segment type XML ids. */
  LINK_SEGMENT_TYPE_XML_ID("org.goplanit.output.property.LinkSegmentTypeXmlIdOutputProperty");

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OutputPropertyType.class.getCanonicalName());

  /** Fully qualified output property class name. */
  private final String value;

  /**
   * Constructor
   * 
   * @param v value string
   */
  OutputPropertyType(String v) {
    value = v;
  }

  /**
   * Return the String value associated with this enumeration value (the fully qualified class name)
   * 
   * @return the class name associated with this enumeration value
   */
  public String value() {
    return value;
  }

  /**
   * Returns the enumeration value associated with the specified class name
   * 
   * @param value the specified class name
   * @return the enumeration value associated with this class name
   */
  public static OutputPropertyType fromValue(String value) {
    for (OutputPropertyType outputProperty : OutputPropertyType.values()) {
      if (outputProperty.value.equals(value)) {
        return outputProperty;
      }
    }
    throw new IllegalArgumentException(value);
  }

  /**
   * Returns the enumeration value associated with a specified header name (the header name in input and output files)
   * 
   * @param name the header name
   * @return the enumeration associated with the specified header name
   * @throws PlanItException if the name is not associated with any output property
   */
  public static OutputPropertyType fromHeaderName(final String name) throws PlanItException {
    String strippedName = name.stripLeading().stripTrailing();
    try {
      for (OutputPropertyType outputProperty : OutputPropertyType.values()) {
        Class<?> entityClass = Class.forName(outputProperty.value);
        OutputProperty baseOutputProperty = (OutputProperty) entityClass.getDeclaredConstructor().newInstance();
        if (baseOutputProperty.getName().equals(strippedName)) {
          return outputProperty;
        }
      }
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException(e);
    }
    throw new PlanItException("The header name " + strippedName + " is not associated with any output property");
  }

}
