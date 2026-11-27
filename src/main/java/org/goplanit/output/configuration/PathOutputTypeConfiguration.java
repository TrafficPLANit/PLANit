package org.goplanit.output.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  private PathOutputIdentificationType pathStringIdType;

  /**
   * Determine how an origin-destination cell is being identified in the output formatter
   * todo: replace with enum, int is ugly
   *
   * @param outputKeyProperties array of output key property types
   * @return the value of the identification type determined
   */
  private int findOdIdentificationMethod(OutputProperty[] outputKeyProperties) {
    Set<OutputPropertyType> outputKeyPropertyList =
        Arrays.stream(outputKeyProperties).map(OutputProperty::getOutputPropertyType).collect(Collectors.toSet());
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
    initialiseDefaultOutputProperties();
    pathStringIdType = PathOutputIdentificationType.LINK_SEGMENT_XML_ID;
  }

  /**
   * Set the default output properties for the path output configuration
   */
  private void initialiseDefaultOutputProperties() {
    // add default output properties

    // keys - Note that path id is a key since there may be multiple paths per origin-destination-mode-time period combination
    addProperty(OutputPropertyType.PATH_ID);
    addProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
    addProperty(OutputPropertyType.MODE_XML_ID);
    addProperty(OutputPropertyType.ORIGIN_ZONE_XML_ID);
    addProperty(OutputPropertyType.DESTINATION_ZONE_XML_ID);

    // value
    addProperty(OutputPropertyType.PATH_STRING);
  }

  /**
   * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
   * 
   * @param originalOutputKeyProperties array of output key property types
   * @return array of keys to be used (null if the list is not valid)
   */
  @Override
  public OutputProperty[] validateAndFilterKeyProperties(OutputProperty[] originalOutputKeyProperties) {
    //TODO: ugly code, rewriting the output properties, should be done nicer at some point

    List<OutputProperty> filteredOutputKeyProperties = new ArrayList<>(3);
    boolean valid = true;
    try {
      switch (findOdIdentificationMethod(originalOutputKeyProperties)) {
      case ORIGIN_DESTINATION_ID_IDENTIFICATION:
        filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_ID));
        filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_ID));
        break;
      case ORIGIN_DESTINATION_XML_ID_IDENTIFICATION:
        filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_XML_ID));
        filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_XML_ID));
        break;
      case ORIGIN_DESTINATION_EXTERNAL_ID_IDENTIFICATION:
        filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.ORIGIN_ZONE_EXTERNAL_ID));
        filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.DESTINATION_ZONE_EXTERNAL_ID));
        break;
      default:
        LOGGER.warning("Configured keys cannot identify origin-destination cell in the skim matrix");
        valid = false;
        filteredOutputKeyProperties = null;
      }
    } catch (Exception e) {
      valid = false;
      LOGGER.warning(e.getMessage());
      LOGGER.warning("Invalid keys encountered for identifying path data");
    }

    /* keep the path id key if present */
    if (valid && OutputProperty.containsPropertyOfType(originalOutputKeyProperties,OutputPropertyType.PATH_ID)) {
      filteredOutputKeyProperties.add(OutputProperty.of(OutputPropertyType.PATH_ID));
    }
    return filteredOutputKeyProperties.toArray(new OutputProperty[filteredOutputKeyProperties.size()]);
  }

  /**
   * Set the path id type
   * 
   * @param pathIdType the path output type
   * @throws PlanItException thrown if there is an error
   */
  public void setPathIdentificationType(PathOutputIdentificationType pathIdType) throws PlanItException {
    this.pathStringIdType = pathIdType;
  }

  /**
   * Get the path id type
   * 
   * @return the path id type
   */
  public PathOutputIdentificationType getPathIdentificationType() {
    return pathStringIdType;
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
    case ORIGIN_ZONE_EXTERNAL_ID:
    case ORIGIN_ZONE_XML_ID:
    case ORIGIN_ZONE_ID:
    case RUN_ID:
    case TIME_PERIOD_EXTERNAL_ID:
    case TIME_PERIOD_XML_ID:
    case TIME_PERIOD_ID:
    case PATH_ID:
    case PATH_STRING:
    case PATH_GEOMETRY:
      return true;
    default:
      LOGGER.warning("Tried to add " + baseOutputProperty.getName() + " as an output property, not registered for Path output.  This will be ignored");
    }
    return false;
  }

}
