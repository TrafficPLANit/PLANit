package org.goplanit.output.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * The configuration for the origin-destination output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <ul>
 * <li>TIME_PERIOD_XML_ID</li>
 * <li>MODE_XML_ID</li>
 * <li>ORIGIN_ZONE_XML_ID
 * <li>
 * <li>DESTINATION_ZONE_XML_ID</li>
 * <li>OD_COST</li>
 * </ul>
 * 
 * 
 * @author markr
 *
 */
public class OdOutputTypeConfiguration extends OutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  /** od identification method by internal id flag */
  private static final int ORIGIN_DESTINATION_ID_IDENTIFICATION = 1;
  /** od identification method by Xml id flag */
  private static final int ORIGIN_DESTINATION_XML_ID_IDENTIFICATION = 2;
  /** od identification method by external id flag */
  private static final int ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION = 3;
  /** od identification method unknown flag */
  private static final int ORIGIN_DESTINATION_NOT_IDENTIFIED = 4;

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
  public OdOutputTypeConfiguration() {
    super(OutputType.OD);
    // add default sub output types (OD - SKIM - COST);
    activeSubOutputTypes.add(OdSkimSubOutputType.COST);
    // add default output properties
    addProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
    addProperty(OutputPropertyType.MODE_XML_ID);
    addProperty(OutputPropertyType.ORIGIN_ZONE_XML_ID);
    addProperty(OutputPropertyType.DESTINATION_ZONE_XML_ID);
    addProperty(OutputPropertyType.OD_COST);
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
      case OdOutputTypeConfiguration.ORIGIN_DESTINATION_ID_IDENTIFICATION:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_ID);
        valid = true;
        break;
      case OdOutputTypeConfiguration.ORIGIN_DESTINATION_XML_ID_IDENTIFICATION:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_XML_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_XML_ID);
        valid = true;
        break;
      case OdOutputTypeConfiguration.ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION:
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
      LOGGER.warning("Invalid keys encountered for identifying od data ");
    }
    if (valid) {
      return outputKeyPropertiesArray;
    }
    return null;
  }

  /**
   * Activate an ODSkimOutputType for this output type configuration
   * 
   * @param odSkimOutputType ODSkimOutputType to be activated
   */
  public void activateOdSkimOutputType(OdSkimSubOutputType odSkimOutputType) {
    activateSubOutputType(odSkimOutputType);
  }

  /**
   * Deactivate an ODSkimOutputType for this output type configuration
   * 
   * @param odSkimOutputType ODSkimOutputType to be deactivated
   */
  public void deactivateOdSkimOutputType(OdSkimSubOutputType odSkimOutputType) {
    deactivateSubOutputType(odSkimOutputType);
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
    case DESTINATION_ZONE_EXTERNAL_ID:
      return true;
    case DESTINATION_ZONE_XML_ID:
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
    case OD_COST:
      return true;
    case ORIGIN_ZONE_EXTERNAL_ID:
      return true;
    case ORIGIN_ZONE_XML_ID:
      return true;
    case ORIGIN_ZONE_ID:
      return true;
    case RUN_ID:
      return true;
    case TIME_PERIOD_EXTERNAL_ID:
      return true;
    case TIME_PERIOD_XML_ID:
      return true;
    case TIME_PERIOD_ID:
      return true;
    default:
      LOGGER.warning("tried to add " + baseOutputProperty.getName() + " as an output property, which is inappropriate for Origin-Destination output. This will be ignored");
    }
    return false;
  }

}
