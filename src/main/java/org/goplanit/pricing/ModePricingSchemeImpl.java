package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.Set;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.pricing.ModePricingScheme;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;

/**
 * A mode scheme: a charge for some modes on a whole class of road or the whole network, such as a truck charge per km
 * on every motorway
 * <p>
 * It charges on its own priced link segments rather than on toll roads: the layers, link segment types and link
 * segments it includes, less those it excludes, tested on demand. One use of it is the whole trip, and each mode is
 * charged by at most one mode scheme, which the network pricing checks.
 * </p>
 *
 * @author markr
 */
public class ModePricingSchemeImpl extends PricingSchemeImpl<StartWithPerKmPriceRule> implements ModePricingScheme {

  /** generated UID */
  private static final long serialVersionUID = -5240397712548336172L;

  /** the link segments charged on, as given */
  private final ModePricingSchemeLinkSegmentCoverage linkSegmentCoverage;

  /** the sections */
  private final List<PricingSchemeSection> sections;

  /** the id within mode schemes */
  private long modeSchemeId;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name of the scheme, null for none
   * @param pricedModeSelection the modes charged, as given
   * @param layers of the network the scheme charges on
   * @param linkSegmentCoverage the link segments charged on, as given
   * @param sections of the scheme, may be empty
   * @param pricing what the scheme charges
   */
  protected ModePricingSchemeImpl(
      IdGroupingToken groupId,
      String name,
      PricedModeSelection pricedModeSelection,
      MacroscopicNetworkLayers layers,
      ModePricingSchemeLinkSegmentCoverage linkSegmentCoverage,
      Collection<? extends PricingSchemeSection> sections,
      SchemePricing<StartWithPerKmPriceRule> pricing) {
    super(groupId, name, pricedModeSelection, layers, pricing);
    this.linkSegmentCoverage = linkSegmentCoverage;
    this.sections = new ArrayList<>(sections);
    this.modeSchemeId = IdGenerator.generateId(groupId, MODE_SCHEME_ID_CLASS);
  }

  /**
   * Copy constructor; shallow and deep copies alike hold their own list of the same sections
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ModePricingSchemeImpl(ModePricingSchemeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.linkSegmentCoverage = other.linkSegmentCoverage;
    this.sections = new ArrayList<>(other.sections);
    this.modeSchemeId = other.modeSchemeId;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Its priced mode selection picks from the modes its layers support; a selection naming none charges those the
   * default rule prices, see {@link PricedModeSelection#isPricedByDefault}.
   * </p>
   */
  @Override
  public Set<Mode> getChargedModes() {
    return Collections.unmodifiableSet(getPricedModeSelection()
        .findPricedModesIn(getLayers().getSupportedModes(), PricedModeSelection::isPricedByDefault));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getModePricingSchemeId() {
    return modeSchemeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemeLinkSegmentCoverage getLinkSegmentCoverage() {
    return linkSegmentCoverage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeSection> getSections() {
    return Collections.unmodifiableList(sections);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeSection> getSectionsOrNone() {
    return getSections();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeGate> getGatesOrNone() {
    return List.of();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateSectionAndGateMapping(
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    sections.replaceAll(sectionMapping);
    sections.removeIf(Objects::isNull);
    super.updateSectionAndGateMapping(sectionMapping, gateMapping);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPricedLinkSegment(LinkSegment linkSegment) {
    return linkSegment instanceof MacroscopicLinkSegment &&
        linkSegmentCoverage.contains((MacroscopicLinkSegment) linkSegment);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Whether its coverage considers the layer, then whether it covers the link segment
   * </p>
   */
  @Override
  public boolean isPricedLinkSegment(MacroscopicNetworkLayer layer, LinkSegment linkSegment) {
    var consideredLayers = linkSegmentCoverage.getConsideredLayers();
    return (consideredLayers.isEmpty() || ListUtils.containsByIdentity(consideredLayers, layer)) &&
        isPricedLinkSegment(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Stream<MacroscopicLinkSegment> streamPricedLinkSegments() {
    return linkSegmentCoverage.stream(getLayers());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    this.modeSchemeId = IdGenerator.generateId(tokenId, MODE_SCHEME_ID_CLASS);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemeImpl shallowClone() {
    return new ModePricingSchemeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemeImpl deepClone() {
    return new ModePricingSchemeImpl(this, true);
  }
}
