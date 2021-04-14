package org.planit.output.property;

import java.util.Optional;

import org.planit.output.enums.Type;
import org.planit.utils.mode.Mode;
import org.planit.utils.unit.Units;

public final class ModeXmlIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Mode Xml Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getUnits() {
    return Units.NONE;
  }

  @Override
  public Type getType() {
    return Type.STRING;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.MODE_XML_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

  /**
   * Returns the Xml Id of the current mode
   * 
   * @param mode current mode
   * @return the external Id of the current mode
   */  
  public static Optional<String> getModeXmlId(Mode mode) {
    return Optional.of(mode.getXmlId());
  }

}
