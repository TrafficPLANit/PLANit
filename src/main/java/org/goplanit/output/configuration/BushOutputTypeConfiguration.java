package org.goplanit.output.configuration;

import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The configuration for the bush output type.
 * 
 * The following OutputProperty values are included by default (for each link on the bush):
 * 
 * <ul>
 * <li>MODE_XML_ID</li>
 * <li>LINK_SEGMENT_XML_ID</li>
 * <li>LINK_SEGMENT_GEOMETRY</li>
 * <li>UPSTREAM_NODE_XML_ID</li>
 * <li>UPSTREAM_NODE_GEOMETRY</li>
 * <li>DOWNSTREAM_NODE_XML_ID</li>
 * <li>DOWNSTREAM_NODE_GEOMETRY</li>
 * <li>TIME_PERIOD_XML_ID</li>
 * </ul>
 * 
 * 
 * @author markr
 *
 */
public class BushOutputTypeConfiguration extends SegmentBaseOutputTypeConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BushOutputTypeConfiguration.class.getCanonicalName());

  /**
   * Constructor
   *
   * Define the default output properties here.
   *
   */
  public BushOutputTypeConfiguration(){
    super(OutputType.BUSH);
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

    // add future options here
    switch (baseOutputProperty.getOutputPropertyType()) {
      default:
        LOGGER.warning("Tried to add " + baseOutputProperty.getName() + " as an output property, not registered for Bush output.  This will be ignored");
      }
    return false;
  }

}