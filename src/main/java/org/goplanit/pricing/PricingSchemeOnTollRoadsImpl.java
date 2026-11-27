package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.ExternalIdAbleUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.pricing.PriceRule;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.problem.Problems;

/**
 * A scheme on toll roads: a location or entry-exit scheme, charging on the link segments of one or more toll roads of
 * the network, priced as one
 * <p>
 * For example WestConnex across the M4, Rozelle, M8 and M5 East, or a closed motorway with a ticket system. The
 * scheme charges the modes its roads price, which they share, and on their link segments only: a link segment of
 * another scheme's toll road is tolled, but not priced by this one. A toll road the network removes is taken off the
 * scheme, see {@link #removeTollRoad}.
 * </p>
 *
 * @param <P> the kind of price rule the scheme charges
 * @author markr
 */
public abstract class PricingSchemeOnTollRoadsImpl<P extends PriceRule<P>> extends PricingSchemeImpl<P> {

  /** generated UID */
  private static final long serialVersionUID = -7192380457736184412L;

  /** the toll roads charged on, priced as one */
  private final List<TollRoad> tollRoads;

  /**
   * The modes a toll road prices, of those its layer supports
   *
   * @param tollRoad of the scheme
   * @return the modes it prices
   */
  private Set<Mode> findModesPricedBy(TollRoad tollRoad) {
    return tollRoad.findPricedModes(findLayerTolling(tollRoad.getLinkSegments().get(0)).getSupportedModes());
  }

  /**
   * The modes its toll roads price, which they share
   *
   * @return the modes, empty when it has no toll road
   */
  private Set<Mode> findModesPricedByTollRoads() {
    return tollRoads.isEmpty() ? Set.of() : findModesPricedBy(tollRoads.get(0));
  }

  /**
   * The layer whose toll roads hold a link segment, one index lookup per layer, as a link segment does not know its
   * layer
   *
   * @param linkSegment to look up
   * @return the layer, null when no toll road holds it
   */
  protected MacroscopicNetworkLayer findLayerTolling(LinkSegment linkSegment) {
    return getLayers().stream()
        .filter(layer -> layer.getTollRoads().getByLinkSegment(linkSegment) != null)
        .findFirst()
        .orElse(null);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Toll roads pricing different modes, a mode the scheme names that they do not price, which it does not charge, and
   * an exempt mode they do not price
   * </p>
   */
  @Override
  protected void recordChargedModeProblems(Problems<PricingProblemKind> problems) {
    long differentPricedModes = tollRoads.stream()
        .map(this::findModesPricedBy)
        .distinct()
        .count();
    problems.addIf(differentPricedModes > 1, PricingProblemKind.TOLL_ROADS_PRICE_DIFFERENT_MODES, "", "",
        "toll roads " + ExternalIdAbleUtils.toIdsAsString(tollRoads));
    var pricedModeSelection = getPricedModeSelection();
    var notPriced = pricedModeSelection.findPricedOrExemptedModesNotIn(findModesPricedByTollRoads());
    problems.addIf(!notPriced.isEmpty() && !pricedModeSelection.getPricedModes().isEmpty(),
        PricingProblemKind.MODE_NOT_PRICED_BY_TOLL_ROADS, "", "", "modes " +
        ExternalIdAbleUtils.toIdsAsString(notPriced));
    problems.addIf(!notPriced.isEmpty() && !pricedModeSelection.getExemptedModes().isEmpty(),
        PricingProblemKind.EXEMPT_MODE_NOT_PRICED_BY_TOLL_ROADS, "", "", "modes " +
        ExternalIdAbleUtils.toIdsAsString(notPriced));
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name of the scheme, null for none
   * @param pricedModeSelection the modes charged, as given
   * @param layers of the network the scheme charges on
   * @param tollRoads charged on, priced as one
   * @param pricing what the scheme charges
   */
  protected PricingSchemeOnTollRoadsImpl(
      IdGroupingToken groupId,
      String name,
      PricedModeSelection pricedModeSelection,
      MacroscopicNetworkLayers layers,
      Collection<? extends TollRoad> tollRoads,
      SchemePricing<P> pricing) {
    super(groupId, name, pricedModeSelection, layers, pricing);
    this.tollRoads = new ArrayList<>(tollRoads);
  }

  /**
   * Copy constructor; shallow and deep copies alike hold their own list of the same toll roads
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PricingSchemeOnTollRoadsImpl(PricingSchemeOnTollRoadsImpl<P> other, boolean deepCopy) {
    super(other, deepCopy);
    this.tollRoads = new ArrayList<>(other.tollRoads);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Its priced mode selection picks from the modes its toll roads price; a selection naming none charges them all.
   * </p>
   */
  @Override
  public Set<Mode> getChargedModes() {
    return Collections.unmodifiableSet(
        getPricedModeSelection().findPricedModesIn(findModesPricedByTollRoads(), mode -> true));
  }

  /**
   * Remove a toll road the network removed from those the scheme charges on
   *
   * @param tollRoad removed
   * @return true when the scheme charged on it
   */
  public boolean removeTollRoad(TollRoad tollRoad) {
    return tollRoads.removeIf(held -> held == tollRoad);
  }

  /**
   * The toll roads the scheme charges on, priced as one, referenced, not owned: the toll roads of their layer own them
   *
   * @return unmodifiable list of the toll roads
   */
  public List<TollRoad> getTollRoads() {
    return Collections.unmodifiableList(tollRoads);
  }

  /**
   * {@inheritDoc}
   * <p>
   * One index lookup per layer, of which networks have one today, until a toll road holds it, then whether that road
   * is one of the scheme's own
   * </p>
   */
  @Override
  public boolean isPricedLinkSegment(LinkSegment linkSegment) {
    return getLayers().stream()
        .map(layer -> layer.getTollRoads().getByLinkSegment(linkSegment))
        .filter(Objects::nonNull)
        .findFirst()
        .map(tollRoad -> ListUtils.containsByIdentity(tollRoads, tollRoad))
        .orElse(false);
  }

  /**
   * {@inheritDoc}
   * <p>
   * One index lookup, then whether the road found is one of the scheme's own
   * </p>
   */
  @Override
  public boolean isPricedLinkSegment(MacroscopicNetworkLayer layer, LinkSegment linkSegment) {
    var tollRoad = layer.getTollRoads().getByLinkSegment(linkSegment);
    return tollRoad != null && ListUtils.containsByIdentity(tollRoads, tollRoad);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Stream<MacroscopicLinkSegment> streamPricedLinkSegments() {
    return tollRoads.stream().flatMap(tollRoad -> tollRoad.getLinkSegments().stream());
  }

  /**
   * {@inheritDoc}
   * <p>
   * On top: the scheme charges on some link segment
   * </p>
   */
  @Override
  public Problems<PricingProblemKind> findProblems() {
    var problems = super.findProblems();
    problems.addIf(
        streamPricedLinkSegments().findAny().isEmpty(), PricingProblemKind.CHARGES_ON_NO_LINK_SEGMENT, "",
        "", "toll roads " + ExternalIdAbleUtils.toIdsAsString(tollRoads));
    return problems;
  }
}
