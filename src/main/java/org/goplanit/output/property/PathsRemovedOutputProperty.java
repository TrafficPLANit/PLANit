package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Number of paths removed relevant to context
 */
public final class PathsRemovedOutputProperty extends OutputProperty {


  /** Paths removed. */
  public static final String NAME = "Paths removed";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public PathsRemovedOutputProperty(){}

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
    return DataType.INTEGER;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.PATHS_REMOVED;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
