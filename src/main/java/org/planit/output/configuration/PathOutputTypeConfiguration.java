package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.planit.network.macroscopic.physical.MacroscopicLinkSegmentImpl;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;

/**
 * The configuration for the OD path output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <ul>
 * <li>RUN_ID</li>
 * <li>PATH_ID</li>
 * <li>TIME_PERIOD_XML_ID</li>
 * <li>MODE_XML_ID</li>
 * <li>ORIGIN_ZONE_XML_ID</li>
 * <li>DESTINATION_ZONE_XML_ID</li>
 * <li>PATH_STRING</li>
 * </ul>
 * 
 * 
 * @author markr
 *
 */
public class PathOutputTypeConfiguration extends OutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  private static final int ORIGIN_DESTINATION_ID_IDENTIFICATION = 1;
  private static final int ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION = 2;
  private static final int ORIGIN_DESTINATION_XML_ID_IDENTIFICATION = 3;
  private static final int ORIGIN_DESTINATION_NOT_IDENTIFIED = 4;

  /**
   * Enumeration to specify the type of object to be recorded in the paths
   */
  private PathOutputIdentificationType pathIdType;

  /**
   * Determine how an origin-destination cell is being identified in the output formatter
   * 
   * @param outputKeyProperties array of output key property types
   * @return the value of the identification type determined
   */
  private int findIdentificationMethod(OutputProperty[] outputKeyProperties) {
    List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyProperties);
    if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_ID)) {
      return ORIGIN_DESTINATION_ID_IDENTIFICATION;
    }
    if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_XML_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_XML_ID)) {
      return ORIGIN_DESTINATION_XML_ID_IDENTIFICATION;
    }
    if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID)) {
      return ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION;
    }
    return ORIGIN_DESTINATION_NOT_IDENTIFIED;
  }

  /**
   * Constructor
   * 
   * Define the default output properties here.
   * 
   * @throws PlanItException thrown if there is an error adding the default properties
   */
  public PathOutputTypeConfiguration() throws PlanItException {
    super(OutputType.PATH);
    // add default output properties
    addProperty(OutputProperty.RUN_ID);
    addProperty(OutputProperty.PATH_ID);
    addProperty(OutputProperty.TIME_PERIOD_XML_ID);
    addProperty(OutputProperty.MODE_XML_ID);
    addProperty(OutputProperty.ORIGIN_ZONE_XML_ID);
    addProperty(OutputProperty.DESTINATION_ZONE_XML_ID);
    addProperty(OutputProperty.PATH_STRING);
    pathIdType = PathOutputIdentificationType.LINK_SEGMENT_XML_ID;
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
    case ORIGIN_DESTINATION_ID_IDENTIFICATION:
      outputKeyPropertiesArray = new OutputProperty[2];
      outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_ID;
      outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_ID;
      valid = true;
      break;
    case ORIGIN_DESTINATION_XML_ID_IDENTIFICATION:
      outputKeyPropertiesArray = new OutputProperty[2];
      outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_XML_ID;
      outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_XML_ID;
      valid = true;
      break;
    case ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION:
      outputKeyPropertiesArray = new OutputProperty[2];
      outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_EXTERNAL_ID;
      outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_EXTERNAL_ID;
      valid = true;
      break;
    default:
      LOGGER.warning("configured keys cannot identify origin-destination cell in the skim matrix");
    }
    if (valid) {
      return outputKeyPropertiesArray;
    }
    return null;
  }

  /**
   * Set the path id type
   * 
   * @param pathIdType the path output type
   * @throws PlanItException thrown if there is an error
   */
  public void setPathIdentificationType(PathOutputIdentificationType pathIdType) throws PlanItException {
    this.pathIdType = pathIdType;
  }

  /**
   * Get the path id type
   * 
   * @return the path id type
   */
  public PathOutputIdentificationType getPathIdentificationType() {
    return pathIdType;
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

    case DESTINATION_ZONE_XML_ID:
      return true;
    case DESTINATION_ZONE_EXTERNAL_ID:
      return true;
    case DESTINATION_ZONE_ID:
      return true;
    case ITERATION_INDEX:
      return true;
    case MODE_EXTERNAL_ID:
      return true;
    case MODE_XML_ID:
      return true;
    case MODE_ID:
      return true;
    case PATH_STRING:
      return true;
    case ORIGIN_ZONE_EXTERNAL_ID:
      return true;
    case ORIGIN_ZONE_XML_ID:
      return true;
    case ORIGIN_ZONE_ID:
      return true;
    case RUN_ID:
      return true;
    case PATH_ID:
      return true;
    case TIME_PERIOD_EXTERNAL_ID:
      return true;
    case TIME_PERIOD_XML_ID:
      return true;
    case TIME_PERIOD_ID:
      return true;
    default:
      LOGGER.warning("tried to add " + baseOutputProperty.getName() + " as an ouput property, which is inappropriate for Path output.  This will be ignored");
    }
    return false;
  }

}
