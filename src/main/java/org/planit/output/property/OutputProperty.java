package org.planit.output.property;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;

/**
 * Enumeration of possible output properties
 * 
 * @author gman6028
 *
 */
public enum OutputProperty {

  // @formatter:off
  DENSITY("org.planit.output.property.DensityOutputProperty"), 
  LINK_SEGMENT_ID("org.planit.output.property.LinkSegmentIdOutputProperty"),
  LINK_SEGMENT_XML_ID("org.planit.output.property.LinkSegmentXmlIdOutputProperty"), 
  LINK_SEGMENT_EXTERNAL_ID("org.planit.output.property.LinkSegmentExternalIdOutputProperty"),
  MODE_ID("org.planit.output.property.ModeIdOutputProperty"), 
  MODE_EXTERNAL_ID("org.planit.output.property.ModeExternalIdOutputProperty"),
  MODE_XML_ID("org.planit.output.property.ModeXmlIdOutputProperty"), 
  MAXIMUM_DENSITY("org.planit.output.property.MaximumDensityOutputProperty"),
  MAXIMUM_SPEED("org.planit.output.property.MaximumSpeedOutputProperty"), 
  CALCULATED_SPEED("org.planit.output.property.CalculatedSpeedOutputProperty"),
  FLOW("org.planit.output.property.FlowOutputProperty"), 
  INFLOW("org.planit.output.property.InflowOutputProperty"), 
  OUTFLOW("org.planit.output.property.OutflowOutputProperty"),
  LENGTH("org.planit.output.property.LengthOutputProperty"), 
  UPSTREAM_NODE_ID("org.planit.output.property.UpstreamNodeIdOutputProperty"),
  UPSTREAM_NODE_EXTERNAL_ID("org.planit.output.property.UpstreamNodeExternalIdOutputProperty"), 
  UPSTREAM_NODE_XML_ID("org.planit.output.property.UpstreamNodeXmlIdOutputProperty"),
  DOWNSTREAM_NODE_ID("org.planit.output.property.DownstreamNodeIdOutputProperty"), 
  DOWNSTREAM_NODE_EXTERNAL_ID("org.planit.output.property.DownstreamNodeExternalIdOutputProperty"),
  DOWNSTREAM_NODE_XML_ID("org.planit.output.property.DownstreamNodeXmlIdOutputProperty"), 
  CAPACITY_PER_LANE("org.planit.output.property.CapacityPerLaneOutputProperty"),
  NUMBER_OF_LANES("org.planit.output.property.NumberOfLanesOutputProperty"), 
  LINK_SEGMENT_COST("org.planit.output.property.LinkSegmentCostOutputProperty"),
  OD_COST("org.planit.output.property.OdCostOutputProperty"), 
  DOWNSTREAM_NODE_LOCATION("org.planit.output.property.DownstreamNodeLocationOutputProperty"),
  UPSTREAM_NODE_LOCATION("org.planit.output.property.UpstreamNodeLocationOutputProperty"),
  ITERATION_INDEX("org.planit.output.property.IterationIndexOutputProperty"),
  ORIGIN_ZONE_ID("org.planit.output.property.OriginZoneIdOutputProperty"), 
  ORIGIN_ZONE_EXTERNAL_ID("org.planit.output.property.OriginZoneExternalIdOutputProperty"),
  ORIGIN_ZONE_XML_ID("org.planit.output.property.OriginZoneXmlIdOutputProperty"), 
  DESTINATION_ZONE_ID("org.planit.output.property.DestinationZoneIdOutputProperty"),
  DESTINATION_ZONE_XML_ID("org.planit.output.property.DestinationZoneXmlIdOutputProperty"),
  DESTINATION_ZONE_EXTERNAL_ID("org.planit.output.property.DestinationZoneExternalIdOutputProperty"), 
  TIME_PERIOD_ID("org.planit.output.property.TimePeriodIdOutputProperty"),
  TIME_PERIOD_XML_ID("org.planit.output.property.TimePeriodXmlIdOutputProperty"), 
  TIME_PERIOD_EXTERNAL_ID("org.planit.output.property.TimePeriodExternalIdOutputProperty"),
  RUN_ID("org.planit.output.property.RunIdOutputProperty"), 
  PATH_STRING("org.planit.output.property.PathOutputStringProperty"),
  PATH_ID("org.planit.output.property.PathIdOutputProperty"), 
  VC_RATIO("org.planit.output.property.VCRatioOutputProperty"),
  COST_TIMES_FLOW("org.planit.output.property.CostTimesFlowOutputProperty"), 
  LINK_SEGMENT_TYPE_ID("org.planit.output.property.LinkSegmentTypeIdOutputProperty"),
  LINK_SEGMENT_TYPE_NAME("org.planit.output.property.LinkSegmentTypeNameOutputProperty"), 
  LINK_SEGMENT_TYPE_XML_ID("org.planit.output.property.LinkSegmentTypeXmlIdOutputProperty");

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OutputProperty.class.getCanonicalName());

  private final String value;

  /**
   * Constructor
   * 
   * @param v value string
   */
  OutputProperty(String v) {
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
  public static OutputProperty fromValue(String value) {
    for (OutputProperty outputProperty : OutputProperty.values()) {
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
  public static OutputProperty fromHeaderName(final String name) throws PlanItException {
    String strippedName = name.stripLeading().stripTrailing();
    try {
      for (OutputProperty outputProperty : OutputProperty.values()) {
        Class<?> entityClass = Class.forName(outputProperty.value);
        BaseOutputProperty baseOutputProperty = (BaseOutputProperty) entityClass.getDeclaredConstructor().newInstance();
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
