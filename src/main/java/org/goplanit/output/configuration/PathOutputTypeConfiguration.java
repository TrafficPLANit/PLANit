package org.goplanit.output.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * The configuration for the OD path output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <ul>
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
    Set<OutputPropertyType> outputKeyPropertyList = Arrays.stream(outputKeyProperties).map(op -> op.getOutputPropertyType()).collect(Collectors.toSet());
    if (outputKeyPropertyList.contains(OutputPropertyType.ORIGIN_ZONE_ID) && outputKeyPropertyList.contains(OutputPropertyType.DESTINATION_ZONE_ID)) {
      return ORIGIN_DESTINATION_ID_IDENTIFICATION;
    }
    if (outputKeyPropertyList.contains(OutputPropertyType.ORIGIN_ZONE_XML_ID) && outputKeyPropertyList.contains(OutputPropertyType.DESTINATION_ZONE_XML_ID)) {
      return ORIGIN_DESTINATION_XML_ID_IDENTIFICATION;
    }
    if (outputKeyPropertyList.contains(OutputPropertyType.ORIGIN_ZONE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputPropertyType.DESTINATION_ZONE_EXTERNAL_ID)) {
      return ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION;
    }
    return ORIGIN_DESTINATION_NOT_IDENTIFIED;
  }

  /**
   * Constructor
   * 
   * Define the default output properties here.
   */
  public PathOutputTypeConfiguration(){
    super(OutputType.PATH);
    // add default output properties
    addProperty(OutputPropertyType.PATH_ID);
    addProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
    addProperty(OutputPropertyType.MODE_XML_ID);
    addProperty(OutputPropertyType.ORIGIN_ZONE_XML_ID);
    addProperty(OutputPropertyType.DESTINATION_ZONE_XML_ID);
    addProperty(OutputPropertyType.PATH_STRING);
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
    try {
      switch (findIdentificationMethod(outputKeyProperties)) {
      case ORIGIN_DESTINATION_ID_IDENTIFICATION:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_ID);
        valid = true;
        break;
      case ORIGIN_DESTINATION_XML_ID_IDENTIFICATION:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_XML_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_XML_ID);
        valid = true;
        break;
      case ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_EXTERNAL_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_EXTERNAL_ID);
        valid = true;
        break;
      default:
        LOGGER.warning("configured keys cannot identify origin-destination cell in the skim matrix");
      }
    } catch (Exception e) {
      LOGGER.warning(e.getMessage());
      LOGGER.warning("Invalid keys encountered for identifying path data");
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
  public boolean isOutputPropertyValid(OutputProperty baseOutputProperty) {
    switch (baseOutputProperty.getOutputPropertyType()) {

    case DESTINATION_ZONE_XML_ID:
    case DESTINATION_ZONE_EXTERNAL_ID:
    case DESTINATION_ZONE_ID:
    case ITERATION_INDEX:
    case MODE_EXTERNAL_ID:
    case MODE_XML_ID:
    case MODE_ID:
    case PATH_STRING:
    case ORIGIN_ZONE_EXTERNAL_ID:
    case ORIGIN_ZONE_XML_ID:
    case ORIGIN_ZONE_ID:
    case RUN_ID:
    case PATH_ID:
    case TIME_PERIOD_EXTERNAL_ID:
    case TIME_PERIOD_XML_ID:
    case TIME_PERIOD_ID:
      return true;
    default:
      LOGGER.warning("tried to add " + baseOutputProperty.getName() + " as an ouput property, which is inappropriate for Path output.  This will be ignored");
    }
    return false;
  }

}
