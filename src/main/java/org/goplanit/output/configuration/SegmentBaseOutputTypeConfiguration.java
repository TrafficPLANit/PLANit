package org.goplanit.output.configuration;

import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The configuration for edge segment based output types.
 * 
 * The following OutputProperty values are included by default:
 * 
 * <ul>
 * <li>LINK_SEGMENT_XML_ID</li>
 * <li>LINK_SEGMENT_GEOMETRY</li>
 * <li>UPSTREAM_NODE_XML_ID</li>
 * <li>UPSTREAM_NODE_GEOMETRY</li>
 * <li>DOWNSTREAM_NODE_XML_ID</li>
 * <li>DOWNSTREAM_NODE_GEOMETRY</li>
 * </ul>
 * 
 * 
 * @author markr
 *
 */
public abstract class SegmentBaseOutputTypeConfiguration extends OutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SegmentBaseOutputTypeConfiguration.class.getCanonicalName());

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
    Set<OutputPropertyType> outputKeyPropertyList = Arrays.stream(outputKeyProperties).map(op -> op.getOutputPropertyType()).collect(Collectors.toSet());
    if (outputKeyPropertyList.contains(OutputPropertyType.DOWNSTREAM_NODE_XML_ID) && outputKeyPropertyList.contains(OutputPropertyType.UPSTREAM_NODE_XML_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_NODE_XML_ID;
    }
    if (outputKeyPropertyList.contains(OutputPropertyType.LINK_SEGMENT_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_ID;
    }
    if (outputKeyPropertyList.contains(OutputPropertyType.LINK_SEGMENT_XML_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_XML_ID;
    }
    if (outputKeyPropertyList.contains(OutputPropertyType.LINK_SEGMENT_EXTERNAL_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID;
    }
    if (outputKeyPropertyList.contains(OutputPropertyType.DOWNSTREAM_NODE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputPropertyType.UPSTREAM_NODE_EXTERNAL_ID)) {
      return LINK_SEGMENT_IDENTIFICATION_BY_NODE_EXTERNAL_ID;
    }
    return LINK_SEGMENT_NOT_IDENTIFIED;
  }

  /**
   * Constructor
   *
   * Define the default output properties here.
   *
   */
  public SegmentBaseOutputTypeConfiguration(OutputType outputType){
    super(outputType);
    addProperty(OutputPropertyType.LINK_SEGMENT_XML_ID);
    addProperty(OutputPropertyType.UPSTREAM_NODE_XML_ID);
    addProperty(OutputPropertyType.DOWNSTREAM_NODE_XML_ID);
    addProperty(OutputPropertyType.MODE_XML_ID);
    addProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
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
      case SegmentBaseOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_NODE_XML_ID:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.DOWNSTREAM_NODE_XML_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.UPSTREAM_NODE_XML_ID);
        valid = true;
        break;
      case SegmentBaseOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_NODE_EXTERNAL_ID:
        outputKeyPropertiesArray = new OutputProperty[2];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.DOWNSTREAM_NODE_EXTERNAL_ID);
        outputKeyPropertiesArray[1] = OutputProperty.of(OutputPropertyType.UPSTREAM_NODE_EXTERNAL_ID);
        valid = true;
        break;
      case SegmentBaseOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_ID:
        outputKeyPropertiesArray = new OutputProperty[1];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.LINK_SEGMENT_ID);
        valid = true;
        break;
      case SegmentBaseOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_XML_ID:
        outputKeyPropertiesArray = new OutputProperty[1];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.LINK_SEGMENT_XML_ID);
        valid = true;
        break;
      case SegmentBaseOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID:
        outputKeyPropertiesArray = new OutputProperty[1];
        outputKeyPropertiesArray[0] = OutputProperty.of(OutputPropertyType.LINK_SEGMENT_EXTERNAL_ID);
        valid = true;
        break;
      default:
        LOGGER.warning("Configured keys cannot identify link segments");
      }
    } catch (Exception e) {
      LOGGER.warning(e.getMessage());
      LOGGER.warning("Invalid keys encountered for identifying link segments");
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
  public boolean isOutputPropertyValid(OutputProperty baseOutputProperty) {
    // check base properties of segments that are expected to be supported. Ok if not as derived
    // classes may support more
    switch (baseOutputProperty.getOutputPropertyType()) {
      case ITERATION_INDEX:
      case LENGTH:
      case DOWNSTREAM_NODE_EXTERNAL_ID:
      case DOWNSTREAM_NODE_XML_ID:
      case DOWNSTREAM_NODE_ID:
      case DOWNSTREAM_NODE_GEOMETRY:
      case UPSTREAM_NODE_EXTERNAL_ID:
      case UPSTREAM_NODE_XML_ID:
      case UPSTREAM_NODE_ID:
      case UPSTREAM_NODE_GEOMETRY:
      case LINK_SEGMENT_EXTERNAL_ID:
      case LINK_SEGMENT_XML_ID:
      case LINK_SEGMENT_ID:
      case LINK_SEGMENT_GEOMETRY:
      case RUN_ID:
      case MODE_EXTERNAL_ID:
      case MODE_XML_ID:
      case MODE_ID:
      case TIME_PERIOD_EXTERNAL_ID:
      case TIME_PERIOD_XML_ID:
      case TIME_PERIOD_ID:
        return true;
    }
    return false;
  }

}