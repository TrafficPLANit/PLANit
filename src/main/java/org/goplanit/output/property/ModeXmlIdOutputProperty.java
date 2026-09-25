package org.goplanit.output.property;

import java.util.Optional;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Mode Xml Id.
 */
public final class ModeXmlIdOutputProperty extends OutputProperty {


  /** Mode Xml Id. */
  public static final String NAME = "Mode Xml Id";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public ModeXmlIdOutputProperty(){}

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
  public Unit getDefaultUnit() {
    return Unit.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataType getDataType() {
    return DataType.STRING;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.MODE_XML_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the Xml Id of the current mode
   * 
   * @param mode current mode
   * @return the XML Id of the current mode
   */  
  public static Optional<String> getModeXmlId(Mode mode) {
    return Optional.of(mode.getXmlId());
  }

}
