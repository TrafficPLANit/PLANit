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
  DENSITY("org.goplanit.output.property.DensityOutputProperty"), 
  LINK_SEGMENT_ID("org.goplanit.output.property.LinkSegmentIdOutputProperty"),
  LINK_SEGMENT_XML_ID("org.goplanit.output.property.LinkSegmentXmlIdOutputProperty"), 
  LINK_SEGMENT_EXTERNAL_ID("org.goplanit.output.property.LinkSegmentExternalIdOutputProperty"),
  MODE_ID("org.goplanit.output.property.ModeIdOutputProperty"), 
  MODE_EXTERNAL_ID("org.goplanit.output.property.ModeExternalIdOutputProperty"),
  MODE_XML_ID("org.goplanit.output.property.ModeXmlIdOutputProperty"), 
  MAXIMUM_DENSITY("org.goplanit.output.property.MaximumDensityOutputProperty"),
  MAXIMUM_SPEED("org.goplanit.output.property.MaximumSpeedOutputProperty"), 
  CALCULATED_SPEED("org.goplanit.output.property.CalculatedSpeedOutputProperty"),
  FLOW("org.goplanit.output.property.FlowOutputProperty"), 
  INFLOW("org.goplanit.output.property.InflowOutputProperty"), 
  OUTFLOW("org.goplanit.output.property.OutflowOutputProperty"),
  LENGTH("org.goplanit.output.property.LengthOutputProperty"), 
  UPSTREAM_NODE_ID("org.goplanit.output.property.UpstreamNodeIdOutputProperty"),
  UPSTREAM_NODE_EXTERNAL_ID("org.goplanit.output.property.UpstreamNodeExternalIdOutputProperty"), 
  UPSTREAM_NODE_XML_ID("org.goplanit.output.property.UpstreamNodeXmlIdOutputProperty"),
  DOWNSTREAM_NODE_ID("org.goplanit.output.property.DownstreamNodeIdOutputProperty"), 
  DOWNSTREAM_NODE_EXTERNAL_ID("org.goplanit.output.property.DownstreamNodeExternalIdOutputProperty"),
  DOWNSTREAM_NODE_XML_ID("org.goplanit.output.property.DownstreamNodeXmlIdOutputProperty"), 
  CAPACITY_PER_LANE("org.goplanit.output.property.CapacityPerLaneOutputProperty"),
  NUMBER_OF_LANES("org.goplanit.output.property.NumberOfLanesOutputProperty"), 
  LINK_SEGMENT_COST("org.goplanit.output.property.LinkSegmentCostOutputProperty"),
  OD_COST("org.goplanit.output.property.OdCostOutputProperty"), 
  DOWNSTREAM_NODE_LOCATION("org.goplanit.output.property.DownstreamNodeLocationOutputProperty"),
  UPSTREAM_NODE_LOCATION("org.goplanit.output.property.UpstreamNodeLocationOutputProperty"),
  ITERATION_INDEX("org.goplanit.output.property.IterationIndexOutputProperty"),
  ORIGIN_ZONE_ID("org.goplanit.output.property.OriginZoneIdOutputProperty"), 
  ORIGIN_ZONE_EXTERNAL_ID("org.goplanit.output.property.OriginZoneExternalIdOutputProperty"),
  ORIGIN_ZONE_XML_ID("org.goplanit.output.property.OriginZoneXmlIdOutputProperty"), 
  DESTINATION_ZONE_ID("org.goplanit.output.property.DestinationZoneIdOutputProperty"),
  DESTINATION_ZONE_XML_ID("org.goplanit.output.property.DestinationZoneXmlIdOutputProperty"),
  DESTINATION_ZONE_EXTERNAL_ID("org.goplanit.output.property.DestinationZoneExternalIdOutputProperty"), 
  TIME_PERIOD_ID("org.goplanit.output.property.TimePeriodIdOutputProperty"),
  TIME_PERIOD_XML_ID("org.goplanit.output.property.TimePeriodXmlIdOutputProperty"), 
  TIME_PERIOD_EXTERNAL_ID("org.goplanit.output.property.TimePeriodExternalIdOutputProperty"),
  RUN_ID("org.goplanit.output.property.RunIdOutputProperty"), 
  PATH_STRING("org.goplanit.output.property.PathOutputStringProperty"),
  PATH_ID("org.goplanit.output.property.PathIdOutputProperty"), 
  VC_RATIO("org.goplanit.output.property.VCRatioOutputProperty"),
  COST_TIMES_FLOW("org.goplanit.output.property.CostTimesFlowOutputProperty"), 
  LINK_SEGMENT_TYPE_ID("org.goplanit.output.property.LinkSegmentTypeIdOutputProperty"),
  LINK_SEGMENT_TYPE_NAME("org.goplanit.output.property.LinkSegmentTypeNameOutputProperty"), 
  LINK_SEGMENT_TYPE_XML_ID("org.goplanit.output.property.LinkSegmentTypeXmlIdOutputProperty");

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OutputPropertyType.class.getCanonicalName());

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
