package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Property reflecting the input xml id used when parsing the link segment in native PLANit format.
 * 
 * @author markr
 *
 */
public class LinkSegmentXmlIdOutputProperty extends BaseOutputProperty {

  public final static String NAME = "Link Segment Xml Id";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Units getUnits() {
    return Units.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return Type.STRING;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.LINK_SEGMENT_XML_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
