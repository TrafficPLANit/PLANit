package org.goplanit.output.configuration;

import org.goplanit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The configuration for the (per iteration/time period/mode) SIMULATION output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <ul>
 * <li>TIME_PERIOD_XML_ID</li>
 * <li>MODE_XML_ID</li>
 * <li>ITERATION_INDEX</li>
 * <li>ROUTE_CHOICE_CONVERGENCE_GAP</li>
 * </ul>
 *
 * By default, each iteration is persisted (unlike link and od data for example)
 * 
 * @author markr
 *
 */
public class SimulationOutputTypeConfiguration extends OutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  /**
   * Set the default output properties for the path output configuration
   */
  private void initialiseDefaultOutputProperties() {
    // add default output properties

    // keys - Note that path id is a key since there may be multiple paths per origin-destination-mode-time period combination
    addProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
    addProperty(OutputPropertyType.MODE_XML_ID);
    addProperty(OutputPropertyType.ITERATION_INDEX);

    // value
    addProperty(OutputPropertyType.ROUTE_CHOICE_CONVERGENCE_GAP);
  }

  /**
   * Constructor
   *
   * Define the default output properties here.
   */
  public SimulationOutputTypeConfiguration(){
    super(OutputType.SIMULATION);
    initialiseDefaultOutputProperties();

    /*  simulation info by default is of most use when recorded each iteration as usually we have just a
        single value for each property per iteration, so persist all iterations as the default */
    setPersistOnlyFinalIteration(false);
  }

  /**
   * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
   * 
   * @param originalOutputKeyProperties array of output key property types
   * @return array of keys to be used (null if the list is not valid)
   */
  @Override
  public OutputProperty[] validateAndFilterKeyProperties(OutputProperty[] originalOutputKeyProperties) {
    var outputKeyPropertiesArray = originalOutputKeyProperties;

    // make sure all default keys are present as they are not supposed to be removed
    if(!OutputProperty.containsAnyTimePeriodIdType(originalOutputKeyProperties)){
      outputKeyPropertiesArray = Arrays.copyOf(originalOutputKeyProperties,originalOutputKeyProperties.length + 1);
      outputKeyPropertiesArray[outputKeyPropertiesArray.length-1] = OutputProperty.of(OutputPropertyType.TIME_PERIOD_XML_ID);
      LOGGER.warning("Simulation output configuration is missing time period key property, adding default");
    }

    if(!OutputProperty.containsAnyModeIdType(originalOutputKeyProperties)){
      outputKeyPropertiesArray = Arrays.copyOf(originalOutputKeyProperties,originalOutputKeyProperties.length + 1);
      outputKeyPropertiesArray[outputKeyPropertiesArray.length-1] = OutputProperty.of(OutputPropertyType.MODE_XML_ID);
      LOGGER.warning("Simulation output configuration is missing mode key property, adding default");
    }

    if(!OutputProperty.containsPropertyOfType(originalOutputKeyProperties, OutputPropertyType.ITERATION_INDEX)){
      outputKeyPropertiesArray = Arrays.copyOf(originalOutputKeyProperties,originalOutputKeyProperties.length + 1);
      outputKeyPropertiesArray[outputKeyPropertiesArray.length-1] = OutputProperty.of(OutputPropertyType.ITERATION_INDEX);
      LOGGER.warning("Simulation output configuration is missing iteration index key property, adding it");
    }

    return outputKeyPropertiesArray;
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

    case ITERATION_INDEX:
    case MODE_EXTERNAL_ID:
    case MODE_XML_ID:
    case MODE_ID:
    case RUN_ID:
    case TIME_PERIOD_EXTERNAL_ID:
    case TIME_PERIOD_XML_ID:
    case TIME_PERIOD_ID:
    case ROUTE_CHOICE_CONVERGENCE_GAP:
      case ROUTE_CHOICE_ITERATION_RUN_TIME:
      return true;
    default:
      LOGGER.warning("Tried to add " + baseOutputProperty.getName() + " as an output property, which is " +
              "inappropriate for Simulation output. This will be ignored");
    }
    return false;
  }

}
