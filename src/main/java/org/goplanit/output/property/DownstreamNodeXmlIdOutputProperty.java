package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * output property for node XML id
 */
public final class DownstreamNodeXmlIdOutputProperty extends OutputProperty {

  /** Public no-arg constructor required for reflective instantiation. */
  public DownstreamNodeXmlIdOutputProperty(){}

  /** Downstream Node Xml Id. */
  public static final String NAME = "Downstream Node Xml Id";

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
    return OutputPropertyType.DOWNSTREAM_NODE_XML_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
