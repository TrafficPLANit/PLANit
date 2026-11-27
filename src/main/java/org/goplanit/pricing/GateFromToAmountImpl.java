package org.goplanit.pricing;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.pricing.GateFromToAmount;
import org.goplanit.utils.pricing.PricingSchemeGate;

/**
 * The amount of one use of an entry-exit scheme from one gate to another, in that direction
 * <p>
 * For example 4.90 from the southern entry to the northern exit of a motorway priced by an operator's table.
 * </p>
 *
 * @author markr
 */
public class GateFromToAmountImpl implements GateFromToAmount {

  /** generated UID */
  private static final long serialVersionUID = -4106822735118806342L;

  /** the gate entered */
  private final PricingSchemeGate fromGate;

  /** the gate left */
  private final PricingSchemeGate toGate;

  /** the amount of the use */
  private final double amountInCurrency;

  /**
   * Constructor
   *
   * @param fromGate entered
   * @param toGate left
   * @param amountInCurrency of the use
   * @throws IllegalArgumentException when a gate is missing
   */
  public GateFromToAmountImpl(PricingSchemeGate fromGate, PricingSchemeGate toGate, double amountInCurrency) {
    ExceptionUtils.throwIllegalArgumentIf(
        fromGate == null || toGate == null, "Gate pair refused, it names two gates");
    this.fromGate = fromGate;
    this.toGate = toGate;
    this.amountInCurrency = amountInCurrency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGate getFromGate() {
    return fromGate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGate getToGate() {
    return toGate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getAmountInCurrency() {
    return amountInCurrency;
  }
}
