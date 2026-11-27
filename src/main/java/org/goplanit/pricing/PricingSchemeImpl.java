package org.goplanit.pricing;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.ExternalIdAbleUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.problem.Problems;

/**
 * What every kind of scheme shares: its name, its modes, its pricing, and the rules that hold whatever its layout
 * <p>
 * A location or entry-exit scheme charges on the link segments of its toll roads, priced as one, see
 * {@link PricingSchemeOnTollRoadsImpl}; a mode scheme on its own link segment coverage, such as every motorway
 * of the network, see {@link ModePricingSchemeImpl}. Each kind answers whether it charges on a link segment and which
 * link segments it charges on, and gives its sections or its gates; the rest, where its priced link segments are
 * entered and left and which of its sections a link segment lies in, is worked out here from those answers, so the
 * rest of the pricing does not ask which kind it is. When registered, a scheme first resolves what it was given
 * against the network, see {@link #resolve}, and then is checked against its own rules, see {@link #findProblems()}.
 * </p>
 *
 * @param <P> the kind of price rule the scheme charges
 * @author markr
 */
public abstract class PricingSchemeImpl<P extends PriceRule<P>> extends ExternalIdAbleImpl
    implements PricingScheme {

  /** generated UID */
  private static final long serialVersionUID = 8730249526195418877L;

  /** the layers of the network the scheme charges on */
  private final MacroscopicNetworkLayers layers;

  /** the name, null when it has none */
  private final String name;

  /** the modes charged, as given */
  private final PricedModeSelection pricedModeSelection;

  /** the description, null when it has none */
  private String description;

  /** what the scheme charges */
  private SchemePricing<P> pricing;

  /**
   * Record what in the scheme's priced mode selection does not fit its candidate modes, when it is registered; a kind
   * whose selection always fits records nothing
   *
   * @param problems to record in
   */
  protected void recordChargedModeProblems(Problems<PricingProblemKind> problems) {
  }

  /**
   * The layers of the network the scheme charges on
   *
   * @return the layers
   */
  protected MacroscopicNetworkLayers getLayers() {
    return layers;
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name of the scheme, null for none
   * @param pricedModeSelection the modes charged, as given
   * @param layers of the network the scheme charges on
   * @param pricing what the scheme charges
   */
  protected PricingSchemeImpl(
      IdGroupingToken groupId,
      String name,
      PricedModeSelection pricedModeSelection,
      MacroscopicNetworkLayers layers,
      SchemePricing<P> pricing) {
    super(IdGenerator.generateId(groupId, SCHEME_ID_CLASS));
    this.name = name;
    this.pricedModeSelection = pricedModeSelection;
    this.layers = layers;
    this.pricing = pricing;
  }

  /**
   * Copy constructor. Shallow and deep copies alike refer to the same network and pricing parts; each kind copies its
   * own lists, and pointing those to copied sections and gates is up to the copy of the pricing
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PricingSchemeImpl(PricingSchemeImpl<P> other, boolean deepCopy) {
    super(other);
    this.name = other.name;
    this.description = other.description;
    this.pricedModeSelection = other.pricedModeSelection;
    this.layers = other.layers;
    this.pricing = other.pricing;
  }

  /**
   * Resolve what the scheme was given against the network, the first step of registering it, recording what is
   * dropped: the modes it charges, its pricing limited to them, and its sections' and gates' link segments
   * <p>
   * The modes it charges are worked out when asked, see {@link #getChargedModes}; registering records what in its
   * priced mode selection does not fit, see {@link #recordChargedModeProblems}. A section's link segment the scheme
   * does not charge on is dropped, as is a gate's entry not where its priced link segments are entered, or exit not
   * where they are left. For example a gate given an entry in the middle of the road would never be met by a search
   * spotting entries as it goes.
   * </p>
   *
   * @param sectionContainer the pricing's sections, kept indexed as sections lose link segments
   * @param gateContainer the pricing's gates, kept indexed as gates lose entries and exits
   * @return the problems found
   */
  protected Problems<PricingProblemKind> resolve(
      PricingSchemeSectionsImpl sectionContainer,
      PricingSchemeGatesImpl gateContainer) {
    var problems = new Problems<PricingProblemKind>();
    recordChargedModeProblems(problems);
    if (pricing instanceof SchemePricingImpl) {
      pricing = ((SchemePricingImpl<P>) pricing).createLimitedToModes(getChargedModes(), problems);
    }

    for (var section : getSectionsOrNone()) {
      sectionContainer.update(section, s -> {
        var dropped =
            ((PricingSchemeSectionImpl) s).removeLinkSegmentsMatching(linkSegment -> !isPricedLinkSegment(linkSegment));
        problems.addIf(!dropped.isEmpty(), PricingProblemKind.SECTION_LINK_SEGMENT_NOT_PRICED, "",
            "section " + s.getIdsAsString(), "link segments " + ExternalIdAbleUtils.toIdsAsString(dropped));
      });
    }
    var gates = getGatesOrNone();
    if (!gates.isEmpty()) {
      var entered = findPricedLinkSegmentsEnteringFromOutside();
      var left = findPricedLinkSegmentsLeavingToOutside();
      for (var gate : gates) {
        gateContainer.update(gate, g -> {
          var dropped = ((PricingSchemeGateImpl) g).removeEntriesAndExitsMatching(
              entry -> !ListUtils.containsByIdentity(entered, entry),
              exit -> !ListUtils.containsByIdentity(left, exit));
          problems.addIf(!dropped.isEmpty(), PricingProblemKind.GATE_NOT_AT_EDGE, "", "gate " + g.getIdsAsString(),
              "link segments " + ExternalIdAbleUtils.toIdsAsString(dropped));
        });
      }
    }
    return problems;
  }

  /**
   * The scheme's sections, for any kind of scheme, so the pricing can reach them without asking which kind it is
   *
   * @return unmodifiable list of the sections, empty for an entry-exit scheme
   */
  public abstract List<PricingSchemeSection> getSectionsOrNone();

  /**
   * The scheme's gates, for any kind of scheme, so the pricing can reach them without asking which kind it is
   *
   * @return unmodifiable list of the gates, empty unless an entry-exit scheme
   */
  public abstract List<PricingSchemeGate> getGatesOrNone();

  /**
   * Point the sections and gates, and those the pricing refers to, to others, such as their copies after a copy of the
   * pricing; a section or gate mapped to null is dropped, with the section price rules and gate pairs referring to it.
   * Each kind points its own sections or gates and then calls this for its pricing
   *
   * @param sectionMapping from each section to the one to refer to, null to drop it
   * @param gateMapping from each gate to the one to refer to, null to drop it
   */
  public void updateSectionAndGateMapping(
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    if (pricing instanceof SchemePricingImpl) {
      pricing = ((SchemePricingImpl<P>) pricing).createWithSectionsAndGatesMapped(sectionMapping, gateMapping);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricedModeSelection getPricedModeSelection() {
    return pricedModeSelection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> findPricedLinkSegmentsEnteringFromOutside() {
    return LinkSegmentUtils.findSegmentsEnteringFromOutside(streamPricedLinkSegments().collect(Collectors.toList()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> findPricedLinkSegmentsLeavingToOutside() {
    return LinkSegmentUtils.findSegmentsLeavingToOutside(streamPricedLinkSegments().collect(Collectors.toList()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean sharesPricedLinkSegmentWith(PricingScheme other) {
    return streamPricedLinkSegments().anyMatch(other::isPricedLinkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSection findSection(LinkSegment linkSegment) {
    if (!isPricedLinkSegment(linkSegment)) {
      return null;
    }
    return getSectionsOrNone().stream()
        .filter(section -> section.contains(linkSegment))
        .findFirst()
        .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SchemePricing<P> getPricing() {
    return pricing;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Checks the rules every kind shares: sections not sharing a link segment, nor gates an entry or an exit; each mode
   * in at most one override of the base and of each time band, the time bands' entries not overlapping, and the
   * distance bands neither overlapping nor mixing periods. Pairs of band entries and of distance bands are each
   * compared once. Each kind adds its own.
   * </p>
   */
  @Override
  public Problems<PricingProblemKind> findProblems() {
    var problems = new Problems<PricingProblemKind>();
    ContainerUtils.findEntriesSharedBetween(getSectionsOrNone(), PricingSchemeSection::getLinkSegments).forEach(
        (linkSegment, owners) -> problems.add(PricingProblemKind.SECTIONS_SHARE_LINK_SEGMENT, "", "", "link segment "
            + linkSegment.getIdsAsString() + " in sections " + ExternalIdAbleUtils.toIdsAsString(owners)));
    ContainerUtils.findEntriesSharedBetween(getGatesOrNone(), PricingSchemeGate::getEntryLinkSegments).forEach(
        (linkSegment, owners) -> problems.add(PricingProblemKind.GATES_SHARE_ENTRY_OR_EXIT, "", "", "entry " +
            linkSegment.getIdsAsString() + " of gates " + ExternalIdAbleUtils.toIdsAsString(owners)));
    ContainerUtils.findEntriesSharedBetween(getGatesOrNone(), PricingSchemeGate::getExitLinkSegments).forEach(
        (linkSegment, owners) -> problems.add(PricingProblemKind.GATES_SHARE_ENTRY_OR_EXIT, "", "", "exit " +
            linkSegment.getIdsAsString() + " of gates " + ExternalIdAbleUtils.toIdsAsString(owners)));

    ContainerUtils.findEntriesSharedBetween(pricing.getPriceOverrides(),
        o -> o.getPricedModeOverrides().getPricedModes())
        .forEach((mode, owners) -> problems.add(PricingProblemKind.MODE_IN_TWO_OVERRIDES, "", "",
            "mode " + mode.getIdsAsString()));
    for (var band : pricing.getTimeBands()) {
      ContainerUtils.findEntriesSharedBetween(band.getPriceOverrides(),
          o -> o.getPricedModeOverrides().getPricedModes())
          .forEach((mode, owners) -> problems.add(PricingProblemKind.MODE_IN_TWO_OVERRIDES, "",
              "band " + band.getXmlId(), "mode " + mode.getIdsAsString()));
    }
    ListUtils.getPairCombinations(pricing.getTimeBands()).stream().filter(pair -> pair.first().getEntries().stream()
        .anyMatch(entry -> pair.second().getEntries().stream().anyMatch(entry::overlaps))).forEach(pair ->
        problems.add(PricingProblemKind.TIME_BANDS_OVERLAP, "", "", "bands " + pair.first().getXmlId() + " and " +
            pair.second().getXmlId()));
    ListUtils.getPairCombinations(pricing.getDistanceBands()).stream()
        .filter(pair -> pair.first().getRange().overlaps(pair.second().getRange())).forEach(pair ->
        problems.add(PricingProblemKind.DISTANCE_BANDS_OVERLAP, "", "", "ranges " + pair.first().getRange() +
            " and " + pair.second().getRange()));
    long distanceBandPeriodCount = pricing.getDistanceBands().stream()
        .map(PricingDistanceBand::getPeriod)
        .distinct()
        .count();
    problems.addIf(distanceBandPeriodCount > 1, PricingProblemKind.DISTANCE_BANDS_MIX_PERIODS, "", "", "");
    return problems;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setId(IdGenerator.generateId(tokenId, SCHEME_ID_CLASS));
    return getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract PricingSchemeImpl<P> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract PricingSchemeImpl<P> deepClone();
}
