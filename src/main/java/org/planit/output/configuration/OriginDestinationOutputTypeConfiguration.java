package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.exceptions.PlanItException;

/**
 * The configuration for the origin-destination output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <li>RUN_ID</li>
 * <li>TIME_PERIOD_EXTERNAL_ID</li>
 * <li>MODE_EXTERNAL_ID</li>
 * <li>ORIGIN_ZONE_EXTERNAL_ID<li>
 * <li>DESTINATION_ZONE_EXTERNAL_ID</li>
 * <li>OD_COST</li>
 * 
 * 
 * @author markr
 *
 */
public class OriginDestinationOutputTypeConfiguration extends OutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  /** od identification method by internal id flag */
  private static final int ORIGIN_DESTINATION_ID_IDENTIFICATION = 1;
  /** od identification method by external id flag */
  private static final int ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION = 2;
  /** od identification method unknown flag */
  private static final int ORIGIN_DESTINATION_NOT_IDENTIFIED = 3;

  /**
   * Determine how an origin-destination cell is being identified in the output formatter
   * 
   * @param outputKeyProperties array of output key property types
   * @return the value of the identification type determined
   */
  private int findIdentificationMethod(OutputProperty[] outputKeyProperties) {
    List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyProperties);
    if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_ID) && outputKeyPropertyList.contains(
        OutputProperty.DESTINATION_ZONE_ID)) {
      return ORIGIN_DESTINATION_ID_IDENTIFICATION;
    }
    if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID) && outputKeyPropertyList.contains(
        OutputProperty.DESTINATION_ZONE_EXTERNAL_ID)) {
      return ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION;
    }
    return ORIGIN_DESTINATION_NOT_IDENTIFIED;
  }

  /**
   * Constructor
   * 
   * Define the default output properties here.
   * 
   * @param trafficAssignment TrafficAssignment object whose results are to be reported
   * @throws PlanItException thrown if there is an error adding the default properties
   */
  public OriginDestinationOutputTypeConfiguration(TrafficAssignment trafficAssignment) throws PlanItException {
    super(trafficAssignment, OutputType.OD);
    // add default sub output types (OD - SKIM - COST);
    activeSubOutputTypes.add(ODSkimSubOutputType.COST);
    // add default output properties
    addProperty(OutputProperty.RUN_ID);
    addProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
    addProperty(OutputProperty.MODE_EXTERNAL_ID);
    addProperty(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID);
    addProperty(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID);
    addProperty(OutputProperty.OD_COST);
  }

  /**
   * Validate whether the specified list of keys is valid, and if it is return only the keys which
   * will be used
   * 
   * @param outputKeyProperties array of output key property types
   * @return array of keys to be used (null if the list is not valid)
   */
  @Override
  public OutputProperty[] validateAndFilterKeyProperties(OutputProperty[] outputKeyProperties) {
    OutputProperty[] outputKeyPropertiesArray = null;
    boolean valid = false;
    switch (findIdentificationMethod(outputKeyProperties)) {
      case OriginDestinationOutputTypeConfiguration.ORIGIN_DESTINATION_ID_IDENTIFICATION:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_ID;
        outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_ID;
        valid = true;
        break;
      case OriginDestinationOutputTypeConfiguration.ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION:
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
   * Activate an ODSkimOutputType for this output type configuration
   * 
   * @param odSkimOutputType ODSkimOutputType to be activated
   */
  public void activateOdSkimOutputType(ODSkimSubOutputType odSkimOutputType) {
    activateSubOutputType(odSkimOutputType);
  }

  /**
   * Deactivate an ODSkimOutputType for this output type configuration
   * 
   * @param odSkimOutputType ODSkimOutputType to be deactivated
   */
  public void deactivateOdSkimOutputType(ODSkimSubOutputType odSkimOutputType) {
    deactivateSubOutputType(odSkimOutputType);
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
      case DESTINATION_ZONE_EXTERNAL_ID:
        return true;
      case DESTINATION_ZONE_ID:
        return true;
      case ITERATION_INDEX:
        return true;
      case MODE_EXTERNAL_ID:
        return true;
      case MODE_ID:
        return true;
      case OD_COST:
        return true;
      case ORIGIN_ZONE_EXTERNAL_ID:
        return true;
      case ORIGIN_ZONE_ID:
        return true;
      case RUN_ID:
        return true;
      case TIME_PERIOD_EXTERNAL_ID:
        return true;
      case TIME_PERIOD_ID:
        return true;
      default:
        LOGGER.warning("tried to add " + baseOutputProperty.getName() + " as an ouput property, which is inappropriate for Origin-Destination output. This will be ignored");
    }
    return false;
  }

}
