package org.goplanit.output.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * The configuration for the link output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <ul>
 * <li>MODE_XML_ID</li>
 * <li>FLOW</li>
 * <li>CAPACITY_PER_LANE</li>
 * <li>NUMBER_OF_LANES</li>
 * <li>LENGTH</li>
 * <li>CALCULATED_SPEED</li>
 * <li>DENSITY</li>
 * <li>LINK_SEGMENT_XML_ID</li>
 * <li>LINK_SEGMENT_GEOMETRY</li>
 * <li>UPSTREAM_NODE_XML_ID</li>
 * <li>UPSTREAM_NODE_GEOMETRY</li>
 * <li>DOWNSTREAM_NODE_XML_ID</li>
 * <li>DOWNSTREAM_NODE_GEOMETRY</li>
 * <li>CAPACITY_PER_LANE</li>
 * <li>LINK_COST</li>
 * <li>MAXIMUM_SPEED</li>
 * <li>TIME_PERIOD_XML_ID</li>
 * </ul>
 * 
 * 
 * @author markr
 *
 */
public class LinkOutputTypeConfiguration extends SegmentBaseOutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LinkOutputTypeConfiguration.class.getCanonicalName());

  /**
   * Constructor
   * 
   * Define the default output properties here.
   *
   */
  public LinkOutputTypeConfiguration(){
    super(OutputType.LINK);
    addProperty(OutputPropertyType.FLOW);
    addProperty(OutputPropertyType.CALCULATED_SPEED);
    addProperty(OutputPropertyType.LINK_SEGMENT_COST);
    addProperty(OutputPropertyType.MAXIMUM_SPEED);
  }

  /**
   * Checks the output property type being added in valid for the current output type configuration
   * 
   * @param baseOutputProperty the output property type being added
   * @return true if the output property is valid, false otherwise
   */
  @Override
  public boolean isOutputPropertyValid(OutputProperty baseOutputProperty) {
    if(super.isOutputPropertyValid(baseOutputProperty)){
      return true;
    }
    switch (baseOutputProperty.getOutputPropertyType()) {
    case CALCULATED_SPEED:
    case CAPACITY_PER_LANE:
    case DENSITY:
    case FLOW:
    case INFLOW:
    case OUTFLOW:
    case LINK_SEGMENT_COST:
    case LINK_SEGMENT_TYPE_ID:
    case LINK_SEGMENT_TYPE_NAME:
    case LINK_SEGMENT_TYPE_XML_ID:
    case MAXIMUM_DENSITY:
    case MAXIMUM_SPEED:
    case NUMBER_OF_LANES:
    case VC_RATIO:
    case COST_TIMES_FLOW:
      return true;
    default:
      LOGGER.warning("Tried to add " + baseOutputProperty.getName() + " as an output property, not registered for Link output.  This will be ignored");
    }
    return false;
  }

}