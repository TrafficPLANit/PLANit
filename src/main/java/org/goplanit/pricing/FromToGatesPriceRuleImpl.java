package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.goplanit.utils.pricing.GateFromToAmount;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;

/**
 * The price rule of an entry-exit scheme: an amount per pair of entry and exit gate, a pair left out meaning the price
 * rule it changes gives it
 * <p>
 * A time band's price rule lists only the pairs it changes, and gets the rest from the base price rule. So working out
 * the point price rule for a mode at a moment combines price rules of this kind, which these methods do, each giving a
 * new price rule: this price rule filled in from another, and multiplied by a factor. A full truck table on 50 gates
 * holds 2,450 pairs, so the pairs are kept by from and to gate next to their list, and {@link #findByFromToGate} is one
 * lookup.
 * </p>
 *
 * @author markr
 */
public class FromToGatesPriceRuleImpl implements FromToGatesPriceRule {

  /** generated UID */
  private static final long serialVersionUID = -2417593062488317745L;

  /** the amounts per pair of gates, in the order given */
  private final List<GateFromToAmount> gatePairs;

  /** the same pairs by from gate and then to gate, gates compared by identity */
  private final Map<PricingSchemeGate, Map<PricingSchemeGate, GateFromToAmount>> gatePairsByFromAndToGate;

  /**
   * {@inheritDoc}
   * <p>
   * Each pair the other gives that this one leaves out is taken from it. Pairs keep the other's order, followed by
   * those only this one gives.
   * </p>
   */
  @Override
  public FromToGatesPriceRuleImpl createFilledInFrom(FromToGatesPriceRule fallback) {
    var filledIn = new ArrayList<GateFromToAmount>(fallback.getPricedFromToGates().size());
    for (var fallbackGatePair : fallback.getPricedFromToGates()) {
      var given = findByFromToGate(fallbackGatePair.getFromGate(), fallbackGatePair.getToGate());
      filledIn.add(given == null ? fallbackGatePair : given);
    }
    gatePairs.stream()
        .filter(gatePair -> fallback.findByFromToGate(gatePair.getFromGate(), gatePair.getToGate()) == null)
        .forEach(filledIn::add);
    return new FromToGatesPriceRuleImpl(filledIn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FromToGatesPriceRuleImpl createMultipliedWith(double factor) {
    return new FromToGatesPriceRuleImpl(gatePairs.stream()
        .map(gatePair -> (GateFromToAmount) new GateFromToAmountImpl(gatePair.getFromGate(), gatePair.getToGate(),
            gatePair.getAmountInCurrency() * factor))
        .collect(Collectors.toList()));
  }

  /**
   * {@inheritDoc}
   * <p>
   * A price rule of gate pairs leaves no amount out, so it is given as it is.
   * </p>
   */
  @Override
  public FromToGatesPriceRuleImpl createWithLeftOutAmountsGiven() {
    return this;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Its gate pairs refer to the mapped gates, a gate mapped to null dropping the pairs referring to it; it refers to no
   * section.
   * </p>
   */
  @Override
  public FromToGatesPriceRuleImpl createWithSectionsAndGatesMapped(
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    return new FromToGatesPriceRuleImpl(gatePairs.stream()
        .map(gatePair -> {
          var fromGate = gateMapping.apply(gatePair.getFromGate());
          var toGate = gateMapping.apply(gatePair.getToGate());
          return fromGate == null || toGate == null ? null :
              (GateFromToAmount) new GateFromToAmountImpl(fromGate, toGate, gatePair.getAmountInCurrency());
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList()));
  }

  /**
   * Constructor
   *
   * @param gatePairs the amounts per pair of gates, may be empty
   */
  public FromToGatesPriceRuleImpl(List<? extends GateFromToAmount> gatePairs) {
    this.gatePairs = List.copyOf(gatePairs);
    this.gatePairsByFromAndToGate = new IdentityHashMap<>();
    for (var gatePair : this.gatePairs) {
      gatePairsByFromAndToGate.computeIfAbsent(gatePair.getFromGate(), fromGate -> new IdentityHashMap<>())
          .put(gatePair.getToGate(), gatePair);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<GateFromToAmount> getPricedFromToGates() {
    return gatePairs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GateFromToAmount findByFromToGate(PricingSchemeGate fromGate, PricingSchemeGate toGate) {
    var byToGate = gatePairsByFromAndToGate.get(fromGate);
    return byToGate == null ? null : byToGate.get(toGate);
  }
}
