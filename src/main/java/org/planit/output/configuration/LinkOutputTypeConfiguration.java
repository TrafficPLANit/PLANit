package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;

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
 * <li>LINK_SEGMENT_ID</li>
 * <li>UPSTREAM_NODE_XML_ID</li>
 * <li>UPSTREAM_NODE_LOCATION</li>
 * <li>DOWNSTREAM_NODE_XML_ID</li>
 * <li>DOWNSTREAM_NODE_LOCATION</li>
 * <li>CAPACITY_PER_LANE</li>
 * <li>LINK_COST</li>
 * <li>MODE_XML_ID</li>
 * <li>MAXIMUM_SPEED</li>
 * <li>TIME_PERIOD_XML_ID</li>
 * </ul>
 * 
 * 
 * @author markr
 *
 */
public class LinkOutputTypeConfiguration extends OutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LinkOutputTypeConfiguration.class.getCanonicalName());

  private static final int LINK_SEGMENT_IDENTIFICATION_BY_NODE_XML_ID = 1;
  private static final int LINK_SEGMENT_IDENTIFICATION_BY_NODE_EXTERNAL_ID = 2;
  private static final int LINK_SEGMENT_IDENTIFICATION_BY_ID = 3;
  private static final int LINK_SEGMENT_IDENTIFICATION_BY_XML_ID = 4;
  private static final int LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID = 5;
  private static final int LINK_SEGMENT_NOT_IDENTIFIED = 6;

  /**
   * Determine how a link is being identified in the output formatter
   * 
   * @param outputKeyProperties array of output key property types
   * @return the value of the identification type determined
   */
  private int findIdentificationMethod(OutputProperty[] outputKeyProperties) {
    List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyProperties);
    if (outputKeyPropertyList.contains(OutputProperty.DOWNSTREAM_NODE_XML_ID) && outputKeyPropertyList.contains(OutputProperty.UPSTREAM_NODE_XML_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_NODE_XML_ID;
    }
    if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_ID;
    }
    if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_XML_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_XML_ID;
    }
    if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_EXTERNAL_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID;
    }
    if (outputKeyPropertyList.contains(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_NODE_EXTERNAL_ID;
    }
    return LINK_SEGMENT_NOT_IDENTIFIED;
  }

  /**
   * Constructor
   * 
   * Define the default output properties here.
   * 
   * @throws PlanItException thrown if there is an error adding the default properties
   */
  public LinkOutputTypeConfiguration() throws PlanItException {
    super(OutputType.LINK);
    addProperty(OutputProperty.LINK_SEGMENT_XML_ID);
    addProperty(OutputProperty.UPSTREAM_NODE_XML_ID);
    addProperty(OutputProperty.DOWNSTREAM_NODE_XML_ID);
    addProperty(OutputProperty.FLOW);
    addProperty(OutputProperty.CALCULATED_SPEED);
    addProperty(OutputProperty.LINK_SEGMENT_COST);
    addProperty(OutputProperty.MODE_XML_ID);
    addProperty(OutputProperty.MAXIMUM_SPEED);
    addProperty(OutputProperty.TIME_PERIOD_XML_ID);
  }

  /**
   * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
   * 
   * @param outputKeyProperties array of output key property types
   * @return array of keys to be used (null if the list is not valid)
   */
  @Override
  public OutputProperty[] validateAndFilterKeyProperties(OutputProperty[] outputKeyProperties) {
    OutputProperty[] outputKeyPropertiesArray = null;
    boolean valid = false;
    switch (findIdentificationMethod(outputKeyProperties)) {
    case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_NODE_XML_ID:
      outputKeyPropertiesArray = new OutputProperty[2];
      outputKeyPropertiesArray[0] = OutputProperty.DOWNSTREAM_NODE_XML_ID;
      outputKeyPropertiesArray[1] = OutputProperty.UPSTREAM_NODE_XML_ID;
      valid = true;
      break;
    case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_NODE_EXTERNAL_ID:
      outputKeyPropertiesArray = new OutputProperty[2];
      outputKeyPropertiesArray[0] = OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID;
      outputKeyPropertiesArray[1] = OutputProperty.UPSTREAM_NODE_EXTERNAL_ID;
      valid = true;
      break;
    case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_ID:
      outputKeyPropertiesArray = new OutputProperty[1];
      outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_ID;
      valid = true;
      break;
    case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_XML_ID:
      outputKeyPropertiesArray = new OutputProperty[1];
      outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_XML_ID;
      valid = true;
      break;
    case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID:
      outputKeyPropertiesArray = new OutputProperty[1];
      outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
      valid = true;
      break;
    default:
      LOGGER.warning("configured keys cannot identify link segments");
    }
    if (valid) {
      return outputKeyPropertiesArray;
    }
    return null;
  }

  /**
   * Checks the output property type being added in valid for the current output type configuration
   * 
   * @param baseOutputProperty the output property type being added
   * @return true if the output property is valid, false otherwise
   */
  @Override
  public boolean isOutputPropertyValid(BaseOutputProperty baseOutputProperty) {
    switch (baseOutputProperty.getOutputProperty()) {
    case CALCULATED_SPEED:
      return true;
    case CAPACITY_PER_LANE:
      return true;
    case DENSITY:
      return true;
    case DOWNSTREAM_NODE_EXTERNAL_ID:
      return true;
    case DOWNSTREAM_NODE_XML_ID:
      return true;
    case DOWNSTREAM_NODE_ID:
      return true;
    case DOWNSTREAM_NODE_LOCATION:
      return true;
    case UPSTREAM_NODE_EXTERNAL_ID:
      return true;
    case UPSTREAM_NODE_XML_ID:
      return true;
    case UPSTREAM_NODE_ID:
      return true;
    case UPSTREAM_NODE_LOCATION:
      return true;
    case FLOW:
      return true;
    case ITERATION_INDEX:
      return true;
    case LENGTH:
      return true;
    case LINK_SEGMENT_COST:
      return true;
    case LINK_SEGMENT_EXTERNAL_ID:
      return true;
    case LINK_SEGMENT_XML_ID:
      return true;
    case LINK_SEGMENT_ID:
      return true;
    case MAXIMUM_DENSITY:
      return true;
    case MAXIMUM_SPEED:
      return true;
    case MODE_EXTERNAL_ID:
      return true;
    case MODE_XML_ID:
      return true;
    case MODE_ID:
      return true;
    case NUMBER_OF_LANES:
      return true;
    case RUN_ID:
      return true;
    case TIME_PERIOD_EXTERNAL_ID:
      return true;
    case TIME_PERIOD_XML_ID:
      return true;
    case TIME_PERIOD_ID:
      return true;
    case VC_RATIO:
      return true;
    case COST_TIMES_FLOW:
      return true;
    case LINK_TYPE:
      return true;
    default:
      LOGGER.warning("tried to add " + baseOutputProperty.getName() + " as an ouput property, which is inappropriate for Link output.  This will be ignored");
    }
    return false;
  }

}