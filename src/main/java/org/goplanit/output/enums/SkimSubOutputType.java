package org.goplanit.output.enums;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.unit.Unit;

/**
 * Enumeration of possible values of OD Skim Output Type, each with the unit a PLANit model holds its skims in
 * <p>
 * A skim matrix holds the unit of its values, and a skim of a type can only be in a unit that measures what the type
 * is: a distance skim in km or metres, a time skim in hours or minutes. The type gives the unit a skim takes unless
 * another is given, and refuses one that does not fit, so a skim of 3 is never read as km when it holds minutes.
 * </p>
 *
 * @author gman6028
 *
 */
public enum SkimSubOutputType implements SubOutputTypeEnum {

  /** No skim sub-output type selected. */
  NONE("None", Unit.NONE),
  /** Travel time skim output. */
  TRAVEL_TIME("TravelTime", Unit.HOUR),
  /** Distance skim output. */
  DISTANCE("Distance", Unit.KM),
  /** Generalized or monetary cost skim output, in hours as the costs of a PLANit model are. */
  COST("Cost", Unit.HOUR);

  /** String label for this skim sub-output type. */
  private final String value;

  /** unit a skim of this type holds its values in unless another is given */
  private final Unit defaultUnit;

  /**
   * Constructor.
   *
   * @param v string label for the skim sub-output type
   * @param defaultUnit unit a skim of this type holds its values in unless another is given
   */
  SkimSubOutputType(String v, Unit defaultUnit) {
    value = v;
    this.defaultUnit = defaultUnit;
  }

  /**
   * Access the string label for this skim sub-output type.
   *
   * @return skim sub-output type label
   */
  public String value() {
    return value;
  }

  /**
   * The unit a skim of this type holds its values in unless another is given: the unit a PLANit model works in, km for
   * a distance and hours for a time or a cost
   *
   * @return the unit
   */
  public Unit getDefaultUnit() {
    return defaultUnit;
  }

  /**
   * Refuse a unit a skim of this type cannot hold its values in, one its default unit cannot be converted to, such as
   * minutes for a distance
   *
   * @param unit to check, not null
   */
  public void verifyUnit(Unit unit) {
    PlanItRunTimeException.throwIfNull(unit, "Unit of a skim of type %s is null, not allowed", value);
    PlanItRunTimeException.throwIf(!defaultUnit.canConvertTo(unit), "Unit %s does not fit a skim of type %s", unit,
        value);
  }
}
