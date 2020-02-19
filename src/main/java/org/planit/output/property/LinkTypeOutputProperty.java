package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class LinkTypeOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Link Type";
  
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
    return OutputProperty.LINK_TYPE;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}