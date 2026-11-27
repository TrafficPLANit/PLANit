package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.pricing.LocationPricingScheme;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;

/**
 * A location scheme: one or more toll roads priced as one, by a start per use and a rate per km, with prices per
 * section
 * <p>
 * For example WestConnex, one flagfall and one cap across the M4, Rozelle, M8 and M5 East, or a cordon, whose
 * crossings are a toll road of the network. Its sections hold what is priced apart, such as a bridge with a start of
 * its own.
 * </p>
 *
 * @author markr
 */
public class LocationPricingSchemeImpl extends PricingSchemeOnTollRoadsImpl<StartWithPerKmPriceRule>
    implements LocationPricingScheme {

  /** generated UID */
  private static final long serialVersionUID = 4189025563107733962L;

  /** the sections */
  private final List<PricingSchemeSection> sections;

  /** the id within location schemes */
  private long locationSchemeId;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name of the scheme, null for none
   * @param pricedModeSelection the modes charged, as given
   * @param layers of the network the scheme charges on
   * @param tollRoads charged on, priced as one
   * @param sections of the scheme, may be empty
   * @param pricing what the scheme charges
   */
  protected LocationPricingSchemeImpl(
      IdGroupingToken groupId,
      String name,
      PricedModeSelection pricedModeSelection,
      MacroscopicNetworkLayers layers,
      Collection<? extends TollRoad> tollRoads,
      Collection<? extends PricingSchemeSection> sections,
      SchemePricing<StartWithPerKmPriceRule> pricing) {
    super(groupId, name, pricedModeSelection, layers, tollRoads, pricing);
    this.sections = new ArrayList<>(sections);
    this.locationSchemeId = IdGenerator.generateId(groupId, LOCATION_SCHEME_ID_CLASS);
  }

  /**
   * Copy constructor; shallow and deep copies alike hold their own list of the same sections
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected LocationPricingSchemeImpl(LocationPricingSchemeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.sections = new ArrayList<>(other.sections);
    this.locationSchemeId = other.locationSchemeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLocationSchemeId() {
    return locationSchemeId;
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
  public long recreateManagedIds(IdGroupingToken tokenId) {
    this.locationSchemeId = IdGenerator.generateId(tokenId, LOCATION_SCHEME_ID_CLASS);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocationPricingSchemeImpl shallowClone() {
    return new LocationPricingSchemeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocationPricingSchemeImpl deepClone() {
    return new LocationPricingSchemeImpl(this, true);
  }
}
