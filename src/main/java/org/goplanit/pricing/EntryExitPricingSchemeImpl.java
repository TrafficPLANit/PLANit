package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.pricing.EntryExitPricingScheme;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.problem.Problems;
import org.goplanit.algorithms.shortest.ShortestPathSearchUtils;

/**
 * An entry-exit scheme: one or more toll roads priced as one, by a table of amounts per pair of entry and exit gate
 * <p>
 * For example a closed motorway with a ticket system: a use runs from an entry gate to an exit gate and costs the
 * amount its table gives for that pair. Its gates must sit where its priced link segments are entered and left, and
 * each pair a mode can travel between must be priced, which it checks when registered.
 * </p>
 *
 * @author markr
 */
public class EntryExitPricingSchemeImpl extends PricingSchemeOnTollRoadsImpl<FromToGatesPriceRule>
    implements EntryExitPricingScheme {

  /** generated UID */
  private static final long serialVersionUID = -3374104965226801935L;

  /** the gates */
  private final List<PricingSchemeGate> gates;

  /** the id within entry-exit schemes */
  private long entryExitSchemeId;

  /**
   * Whether the base price rule or a time band's price rule lists the pair of gates
   *
   * @param fromGate entered
   * @param toGate left
   * @return true when a price rule lists it
   */
  private boolean isPairListed(PricingSchemeGate fromGate, PricingSchemeGate toGate) {
    var pricing = getPricing();
    var basePriceRule = pricing.getBasePriceRule();
    return (basePriceRule != null && basePriceRule.findByFromToGate(fromGate, toGate) != null) ||
        pricing.getTimeBands().stream().anyMatch(band -> band.getPriceRule() != null &&
            band.getPriceRule().findByFromToGate(fromGate, toGate) != null);
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name of the scheme, null for none
   * @param pricedModeSelection the modes charged, as given
   * @param layers of the network the scheme charges on
   * @param tollRoads charged on, priced as one
   * @param gates of the scheme
   * @param pricing what the scheme charges
   */
  protected EntryExitPricingSchemeImpl(
      IdGroupingToken groupId,
      String name,
      PricedModeSelection pricedModeSelection,
      MacroscopicNetworkLayers layers,
      Collection<? extends TollRoad> tollRoads,
      Collection<? extends PricingSchemeGate> gates,
      SchemePricing<FromToGatesPriceRule> pricing) {
    super(groupId, name, pricedModeSelection, layers, tollRoads, pricing);
    this.gates = new ArrayList<>(gates);
    this.entryExitSchemeId = IdGenerator.generateId(groupId, ENTRY_EXIT_SCHEME_ID_CLASS);
  }

  /**
   * Copy constructor; shallow and deep copies alike hold their own list of the same gates
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected EntryExitPricingSchemeImpl(EntryExitPricingSchemeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.gates = new ArrayList<>(other.gates);
    this.entryExitSchemeId = other.entryExitSchemeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getEntryExitSchemeId() {
    return entryExitSchemeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeGate> getGates() {
    return Collections.unmodifiableList(gates);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeSection> getSectionsOrNone() {
    return List.of();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeGate> getGatesOrNone() {
    return getGates();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateSectionAndGateMapping(
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    gates.replaceAll(gateMapping);
    gates.removeIf(Objects::isNull);
    super.updateSectionAndGateMapping(sectionMapping, gateMapping);
  }

  /**
   * {@inheritDoc}
   * <p>
   * On top: at least one gate, and each pair of an entry gate and another gate's exit that a charged mode can travel
   * between along the priced link segments, banned turns respected, listed by a price rule. Each pair is searched once
   * per entry and mode, from the entry over the priced link segments only, and reported once.
   * </p>
   */
  @Override
  public Problems<PricingProblemKind> findProblems() {
    var problems = super.findProblems();
    problems.addIf(gates.isEmpty(), PricingProblemKind.NO_GATES, "", "", "");
    var reported = new ArrayList<Pair<PricingSchemeGate, PricingSchemeGate>>();
    for (var fromGate : gates) {
      for (var entry : fromGate.getEntryLinkSegments()) {
        var layer = findLayerTolling(entry);
        for (var mode : getChargedModes()) {
          var reached = ShortestPathSearchUtils.findShortestConnectionsForMode(layer, entry, mode,
              linkSegment -> isPricedLinkSegment(layer, linkSegment),
              linkSegment -> isPricedLinkSegment(layer, linkSegment), true).getReached();
          for (var toGate : gates) {
            boolean connected = toGate != fromGate && toGate.getExitLinkSegments().stream()
                .anyMatch(exit -> ListUtils.containsByIdentity(reached, exit));
            if (connected && !isPairListed(fromGate, toGate) && reported.stream()
                .noneMatch(pair -> pair.first() == fromGate && pair.second() == toGate)) {
              reported.add(Pair.of(fromGate, toGate));
              problems.add(
                  PricingProblemKind.PAIR_WITHOUT_PRICE, "", "",
                  "from gate " + fromGate.getIdsAsString() + " to gate " + toGate.getIdsAsString());
            }
          }
        }
      }
    }
    return problems;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    this.entryExitSchemeId = IdGenerator.generateId(tokenId, ENTRY_EXIT_SCHEME_ID_CLASS);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EntryExitPricingSchemeImpl shallowClone() {
    return new EntryExitPricingSchemeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EntryExitPricingSchemeImpl deepClone() {
    return new EntryExitPricingSchemeImpl(this, true);
  }
}
