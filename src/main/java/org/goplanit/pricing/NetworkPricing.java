package org.goplanit.pricing;

import java.io.Serializable;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.goplanit.component.PlanitComponent;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.pricing.modifier.NetworkPricingModifierImpl;
import org.goplanit.pricing.modifier.event.handler.UpdatePricingOnNetworkChangeHandler;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierListener;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.pricing.EntryExitPricingScheme;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.pricing.LocationPricingScheme;
import org.goplanit.utils.pricing.ModePricingScheme;
import org.goplanit.utils.pricing.PriceLookupTable;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.pricing.PriceCapTotals;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingProblemOutcome;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.problem.Problems;

/**
 * The prices charged for using a network: its schemes of three kinds, their sections and gates, and caps covering
 * several schemes, in one currency and at one year's price level
 * <p>
 * Prices change yearly and are a scenario lever, so they are a component of their own that refers to the network, as
 * the zoning and the service network do, rather than part of the network file. The network holds the toll roads, the
 * infrastructure; the pricing says what using them costs. A network without a network pricing behaves as before.
 * Schemes are added through the factory of their kind, which resolves and checks each one and adds it unless a problem
 * refuses it; the checks across schemes, such as one mode scheme per mode, are this component's, see
 * {@link #findProblemsAcrossSchemes(PricingScheme)}.
 * </p>
 *
 * @author markr
 */
public class NetworkPricing extends PlanitComponent<NetworkPricing> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -7046812339574412863L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NetworkPricing.class.getCanonicalName());

  /** the network the pricing refers to */
  private final MacroscopicNetwork parentNetwork;

  /** the token the ids of its entities are generated with */
  private final IdGroupingToken pricingToken;

  /** the mode schemes */
  private final ModePricingSchemesImpl modeSchemes;

  /** the location schemes */
  private final LocationPricingSchemesImpl locationSchemes;

  /** the entry-exit schemes */
  private final EntryExitPricingSchemesImpl entryExitSchemes;

  /** the sections of all schemes */
  private final PricingSchemeSectionsImpl sections;

  /** the gates of all schemes */
  private final PricingSchemeGatesImpl gates;

  /** the caps */
  private final PriceCapsImpl caps;

  /** removes schemes and recreates their ids */
  private final NetworkPricingModifierImpl modifier;

  /** keeps the pricing in step with changes to the network */
  private final UpdatePricingOnNetworkChangeHandler networkChangeHandler;

  /** the layer modifiers followed, by identity, so each is listened to once */
  private final List<MacroscopicNetworkLayerModifier> layerModifiersFollowed = new ArrayList<>();

  /** the currency of every amount, null until set */
  private Currency currency;

  /** the price level of every amount, null until set */
  private Year year;

  /**
   * Compute the cumulative price of a use from the amounts its price rule gives, the steps both kinds of price rule
   * share: the amount charged once per use multiplied by the factor of the distance band where the use starts, and the
   * amount for its km by the average factor over its km; then the smaller of their total and the mode's cap per use;
   * then the larger of that and the mode's minimum, unless the distance bands made the use free; then at most what each
   * cap over a period covering the scheme and mode has left, and nothing once a count cap's uses are reached
   * <p>
   * The caps over a period come after the minimum, as a cap is the most paid in its period: with a day cap of 10.00 and
   * 9.00 paid, a use of 3.00 under a minimum of 2.00 pays 1.00. Without totals the use counts as the first in every
   * cap's period.
   * </p>
   *
   * @param scheme used
   * @param mode one the scheme charges
   * @param pricing of the scheme
   * @param distanceTravelledForBinSelectionKm the distance travelled on the scheme in its distance bands' period
   *     before the use, which with the use's own picks the distance bands
   * @param useDistanceKm the distance of the use on the scheme, 0 for a use priced by its gates
   * @param startAmountInCurrency charged once per use, before distance bands: the starts, or the amount of the pair of
   *     gates
   * @param kmAmountInCurrency charged for the use's km, before distance bands, 0 for a use priced by its gates
   * @param capTotals of the traveller, null to count the use as the first in every cap's period
   * @return the cumulative price
   */
  private double computeCumulativePriceFromAmounts(
      PricingScheme scheme,
      Mode mode,
      SchemePricing<?> pricing,
      double distanceTravelledForBinSelectionKm,
      double useDistanceKm,
      double startAmountInCurrency,
      double kmAmountInCurrency,
      PriceCapTotals capTotals) {
    double startFactor = pricing.computeDistanceBandFactor(distanceTravelledForBinSelectionKm);
    double kmFactor = useDistanceKm > 0 ?
        pricing.computeKmTimesDistanceBandFactors(distanceTravelledForBinSelectionKm, useDistanceKm) / useDistanceKm :
        startFactor;
    double amountInCurrency = startAmountInCurrency * startFactor + kmAmountInCurrency * kmFactor;

    double cumulativePrice = Math.min(amountInCurrency, findPriceCapPerUse(scheme, mode));
    boolean madeFreeByDistanceBands = amountInCurrency == 0 && startAmountInCurrency + kmAmountInCurrency > 0;
    if (!madeFreeByDistanceBands) {
      cumulativePrice = Math.max(cumulativePrice, pricing.findModesMinimumPrice(mode));
    }
    for (var cap : caps.getByScheme(scheme)) {
      if (!cap.isPerUse() && cap.appliesTo(mode)) {
        cumulativePrice = cap.computeCappedPrice(cumulativePrice, capTotals);
      }
    }
    return cumulativePrice;
  }

  /**
   * Check a scheme resolved by its factory against its own rules and those across schemes, and register it unless a
   * problem refuses it, the last steps of registering a scheme of any kind. A refused scheme's sections and gates are
   * removed from the pricing, as each belongs to its scheme
   *
   * @param <S> kind of scheme
   * @param scheme resolved
   * @param problems found while resolving it, the others are added
   * @param container of its kind, to register it on
   * @return the scheme, null when a problem refuses it, and every problem found
   */
  protected <S extends PricingSchemeImpl<?>> Pair<S, Problems<PricingProblemKind>> registerUnlessRefused(
      S scheme,
      Problems<PricingProblemKind> problems,
      ManagedIdEntitiesWithForeignKeyIndicesImpl<? super S> container) {

    problems.addAll(scheme.findProblems());
    problems.addAll(findProblemsAcrossSchemes(scheme));
    if (problems.hasAny(kind -> kind.getOutcome() == PricingProblemOutcome.REFUSED)) {
      scheme.getSectionsOrNone().forEach(sections::remove);
      scheme.getGatesOrNone().forEach(gates::remove);
      return Pair.of(null, problems);
    }
    container.register(scheme);
    return Pair.of(scheme, problems);
  }

  /**
   * The most one use of a scheme costs a mode: the lowest amount of the caps per use covering the scheme and the mode
   *
   * @param scheme registered with this network pricing
   * @param mode one the scheme charges
   * @return the cap per use, {@code Double.POSITIVE_INFINITY} when none covers the mode
   */
  protected double findPriceCapPerUse(PricingScheme scheme, Mode mode) {
    return caps.getByScheme(scheme).stream()
        .filter(cap -> cap.isPerUse() && cap.getAmountInCurrency() != null && cap.appliesTo(mode))
        .mapToDouble(PriceCap::getAmountInCurrency)
        .min()
        .orElse(Double.POSITIVE_INFINITY);
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param parentNetwork the network the pricing refers to
   */
  public NetworkPricing(IdGroupingToken groupId, MacroscopicNetwork parentNetwork) {
    super(groupId, NetworkPricing.class);
    this.parentNetwork = parentNetwork;
    /* its entities numbered from 0 by a token of its own, so reading the same input again gives the same ids */
    this.pricingToken = IdGenerator.createIdGroupingToken(this, getId());
    this.modeSchemes = new ModePricingSchemesImpl(pricingToken, this);
    this.locationSchemes = new LocationPricingSchemesImpl(pricingToken, this);
    this.entryExitSchemes = new EntryExitPricingSchemesImpl(pricingToken, this);
    this.sections = new PricingSchemeSectionsImpl(pricingToken);
    this.gates = new PricingSchemeGatesImpl(pricingToken);
    this.caps = new PriceCapsImpl(pricingToken);
    this.modifier = new NetworkPricingModifierImpl(this);
    this.networkChangeHandler = new UpdatePricingOnNetworkChangeHandler(this);
    attachTo(parentNetwork);
  }

  /**
   * Copy constructor. A copy refers to the same network and toll roads; a deep copy holds copies of the schemes,
   * sections, gates and caps, referring to one another
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public NetworkPricing(NetworkPricing other, boolean deepCopy) {
    super(other, deepCopy);
    this.parentNetwork = other.parentNetwork;
    this.pricingToken = other.pricingToken;
    this.currency = other.currency;
    this.year = other.year;
    var sectionMapper = deepCopy ? new ManagedIdDeepCopyMapper<PricingSchemeSection>() : null;
    var gateMapper = deepCopy ? new ManagedIdDeepCopyMapper<PricingSchemeGate>() : null;
    var schemeMapper = deepCopy ? new ManagedIdDeepCopyMapper<PricingScheme>() : null;
    this.sections = new PricingSchemeSectionsImpl(other.sections, deepCopy, sectionMapper);
    this.gates = new PricingSchemeGatesImpl(other.gates, deepCopy, gateMapper);
    this.modeSchemes = new ModePricingSchemesImpl(other.modeSchemes, deepCopy,
        deepCopy ? schemeMapper::accept : null, this);
    this.locationSchemes = new LocationPricingSchemesImpl(other.locationSchemes, deepCopy,
        deepCopy ? schemeMapper::accept : null, this);
    this.entryExitSchemes = new EntryExitPricingSchemesImpl(other.entryExitSchemes, deepCopy,
        deepCopy ? schemeMapper::accept : null, this);
    this.caps = new PriceCapsImpl(other.caps, deepCopy, null);
    if (deepCopy) {
      streamSchemes().forEach(scheme -> ((PricingSchemeImpl<?>) scheme).updateSectionAndGateMapping(
          sectionMapper::getMapping, gateMapper::getMapping));
      caps.updateSchemeMapping(schemeMapper::getMapping);
    }
    this.modifier = new NetworkPricingModifierImpl(this);
    this.networkChangeHandler = new UpdatePricingOnNetworkChangeHandler(this);
    attachTo(parentNetwork);
  }

  /**
   * Follow the changes to a network's layers, so the schemes, sections and gates stay in step when links are broken or
   * removed and toll roads or link segment types removed; done on construction for the network the pricing refers to.
   * The pricing listens to each layer modifier as an internal listener, so it keeps following when the other listeners
   * of a layer are removed
   * <p>
   * The layers the network has now are followed; a layer added later is followed once this is called again. A layer
   * already followed is left as it is
   * </p>
   *
   * @param network to follow
   */
  public void attachTo(MacroscopicNetwork network) {
    for (var layer : network.getTransportLayers()) {
      var layerModifier = layer.getLayerModifier();
      if (!ListUtils.containsByIdentity(layerModifiersFollowed, layerModifier)) {
        layerModifiersFollowed.add(layerModifier);
        layerModifier.addInternalListener((GraphModifierListener) networkChangeHandler);
        layerModifier.addInternalListener((TollRoadModifierListener) networkChangeHandler);
        layerModifier.addInternalListener((LinkSegmentTypeModifierListener) networkChangeHandler);
      }
    }
  }

  /**
   * Stop following a network, e.g. for a copy of this pricing no longer used while the network lives on
   *
   * @param network to stop following
   */
  public void detachFrom(MacroscopicNetwork network) {
    for (var layer : network.getTransportLayers()) {
      var layerModifier = layer.getLayerModifier();
      if (layerModifiersFollowed.removeIf(followed -> followed == layerModifier)) {
        layerModifier.removeInternalListener((GraphModifierListener) networkChangeHandler);
        layerModifier.removeInternalListener((TollRoadModifierListener) networkChangeHandler);
        layerModifier.removeInternalListener((LinkSegmentTypeModifierListener) networkChangeHandler);
      }
    }
  }

  /**
   * The modifier removing schemes and recreating their ids
   *
   * @return the modifier
   */
  public NetworkPricingModifierImpl getModifier() {
    return modifier;
  }

  /**
   * The network the pricing refers to
   *
   * @return the network
   */
  public MacroscopicNetwork getParentNetwork() {
    return parentNetwork;
  }

  /**
   * The currency of every amount
   *
   * @return the currency, null when not set
   */
  public Currency getCurrency() {
    return currency;
  }

  /**
   * Set the currency of every amount
   *
   * @param currency to set
   */
  public void setCurrency(Currency currency) {
    this.currency = currency;
  }

  /**
   * The price level of every amount
   *
   * @return the year, null when not set
   */
  public Year getYear() {
    return year;
  }

  /**
   * Set the price level of every amount
   *
   * @param year to set
   */
  public void setYear(Year year) {
    this.year = year;
  }

  /**
   * The mode schemes
   *
   * @return the mode schemes
   */
  public ModePricingSchemesImpl getModeSchemes() {
    return modeSchemes;
  }

  /**
   * The location schemes
   *
   * @return the location schemes
   */
  public LocationPricingSchemesImpl getLocationSchemes() {
    return locationSchemes;
  }

  /**
   * The entry-exit schemes
   *
   * @return the entry-exit schemes
   */
  public EntryExitPricingSchemesImpl getEntryExitSchemes() {
    return entryExitSchemes;
  }

  /**
   * The schemes of every kind
   *
   * @return the schemes, mode schemes first, then location and entry-exit schemes
   */
  public Stream<PricingScheme> streamSchemes() {
    return Stream.of(modeSchemes.stream(), locationSchemes.stream(), entryExitSchemes.stream()).flatMap(s -> s);
  }

  /**
   * The sections of all schemes
   *
   * @return the sections
   */
  public PricingSchemeSectionsImpl getSections() {
    return sections;
  }

  /**
   * The gates of all schemes
   *
   * @return the gates
   */
  public PricingSchemeGatesImpl getGates() {
    return gates;
  }

  /**
   * The caps
   *
   * @return the caps
   */
  public PriceCapsImpl getCaps() {
    return caps;
  }

  /**
   * The problems of a scheme against the schemes already added, asked by the factories before they add it: a mode
   * charged by a mode scheme already, refused; or a location or entry-exit scheme charging a mode another one charges
   * on a link segment both charge on, reported once per pair, as both are kept
   * <p>
   * Each mode is charged by at most one mode scheme, found through the mode schemes' index by charged mode. Two schemes
   * on toll roads may well overlap, such as two operators' tables sharing a road, but their charges add up, so it is
   * reported.
   * </p>
   *
   * @param candidate scheme, resolved
   * @return the problems found
   */
  public Problems<PricingProblemKind> findProblemsAcrossSchemes(PricingScheme candidate) {
    var problems = new Problems<PricingProblemKind>();
    if (candidate instanceof ModePricingScheme) {
      for (var mode : candidate.getChargedModes()) {
        var other = modeSchemes.getByPricedMode(mode);
        problems.addIf(other != null && other != candidate,
            PricingProblemKind.SECOND_MODE_SCHEME_FOR_MODE, "", "",
            "mode " + mode.getIdsAsString() + " charged by mode scheme " +
            (other == null ? "" : other.getIdsAsString()));
      }
      return problems;
    }
    streamSchemes().filter(other -> other != candidate && !(other instanceof ModePricingScheme))
        .filter(other -> other.getChargedModes().stream().anyMatch(candidate::chargesMode))
        .filter(candidate::sharesPricedLinkSegmentWith)
        .forEach(other -> problems.add(PricingProblemKind.SCHEMES_SHARE_LINK_SEGMENT, "", "",
            "scheme " + other.getIdsAsString()));
    return problems;
  }

  /**
   * A read-only lookup table of a scheme's price rules, for an assignment or a route search asking for them many times
   * <p>
   * Built when asked, once, after any change to the pricing or the network, as it holds the prices as they were then.
   * The network pricing builds it as it holds the scheme's caps: each charged mode's minimum of one use and lowest cap
   * per use go in next to the amounts, which the scheme's kind of price rule decides, see
   * {@link StartWithPerKmPriceLookupTableImpl} and {@link FromToGatesPriceLookupTableImpl}. A mode the scheme does
   * not charge pays nothing.
   * </p>
   *
   * @param scheme registered with this network pricing
   * @return the lookup table, of the scheme's kind of price rule
   */
  public PriceLookupTable createPriceLookupTable(PricingScheme scheme) {
    int modeCount = (int) parentNetwork.getModes().stream()
        .mapToLong(Mode::getId)
        .max()
        .orElse(-1) + 1;
    var modesMinimumPrices = new double[modeCount];
    var priceCapsPerUse = new double[modeCount];
    Arrays.fill(priceCapsPerUse, Double.POSITIVE_INFINITY);
    for (var mode : scheme.getChargedModes()) {
      modesMinimumPrices[(int) mode.getId()] = scheme.getPricing().findModesMinimumPrice(mode);
      priceCapsPerUse[(int) mode.getId()] = findPriceCapPerUse(scheme, mode);
    }
    if (scheme instanceof EntryExitPricingSchemeImpl) {
      return new FromToGatesPriceLookupTableImpl((EntryExitPricingSchemeImpl) scheme, modesMinimumPrices,
          priceCapsPerUse);
    }
    return new StartWithPerKmPriceLookupTableImpl(scheme instanceof ModePricingSchemeImpl ?
        (ModePricingSchemeImpl) scheme : (LocationPricingSchemeImpl) scheme, modesMinimumPrices, priceCapsPerUse);
  }

  /**
   * The cumulative price of one use of a mode or location scheme, as the first use in every cap's period, see
   * {@link #computeCumulativePrice(PricingScheme, Mode, TimeInWeekWithDayType, double, double, Map, PriceCapTotals)}
   *
   * @param scheme a mode or location scheme registered with this network pricing
   * @param mode using it
   * @param moment the use starts at
   * @param distanceTravelledForBinSelectionKm the distance travelled on the scheme in its distance bands' period
   *     before the use, which with the use's own picks the distance bands
   * @param useDistanceKm the distance of the use on the scheme
   * @param sectionDistancesKm the distance of the use in each section it enters, empty when it enters none
   * @return what the use costs, 0 for a mode the scheme does not charge
   */
  public double computeCumulativePrice(
      PricingScheme scheme,
      Mode mode,
      TimeInWeekWithDayType moment,
      double distanceTravelledForBinSelectionKm,
      double useDistanceKm,
      Map<PricingSchemeSection,Double> sectionDistancesKm) {

    return computeCumulativePrice(scheme, mode, moment, distanceTravelledForBinSelectionKm, useDistanceKm,
        sectionDistancesKm, null);
  }

  /**
   * The cumulative price of one use of a mode or location scheme: what the use costs in total, its minimum and caps
   * applied
   * <p>
   * At the moment the use starts: its start once, the rate per km over its distance outside sections, and per section
   * entered its start once and its rate over its distance there. Each km is multiplied by the factor of the distance
   * band it falls in, counting on from the distance already travelled; as the order of the use's km in and out of
   * sections is not given, its km and their rates are spread evenly over the use, which is exact for a use within one
   * distance band or at one rate, and the starts take the factor where the use starts. Then the cap per use, the
   * minimum and the caps over a period, see {@link #computeCumulativePriceFromAmounts}. For example a start of
   * 0.50 and 0.45 per km under a minimum of 2.00: a use of 3 km costs 2.00.
   * </p>
   *
   * @param scheme a mode or location scheme registered with this network pricing
   * @param mode using it
   * @param moment the use starts at
   * @param distanceTravelledForBinSelectionKm the distance travelled on the scheme in its distance bands' period
   *     before the use, which with the use's own picks the distance bands
   * @param useDistanceKm the distance of the use on the scheme
   * @param sectionDistancesKm the distance of the use in each section it enters, empty when it enters none
   * @param capTotals the traveller's totals per cap, null to count the use as the first in every cap's period
   * @return what the use costs, 0 for a mode the scheme does not charge
   */
  public double computeCumulativePrice(
      PricingScheme scheme,
      Mode mode,
      TimeInWeekWithDayType moment,
      double distanceTravelledForBinSelectionKm,
      double useDistanceKm,
      Map<PricingSchemeSection, Double> sectionDistancesKm,
      PriceCapTotals capTotals) {

    if (!scheme.chargesMode(mode)) {
      return 0;
    }
    var pricing = (SchemePricingImpl<StartWithPerKmPriceRule>) (scheme instanceof ModePricingScheme ?
        ((ModePricingScheme) scheme).getPricing() : ((LocationPricingScheme) scheme).getPricing());
    var priceRule = pricing.findPointPriceRuleInTimeBand(mode, pricing.findTimeBand(moment));
    double startAmountInCurrency = priceRule.getStartAmountInCurrency();
    double kmAmountInCurrency = priceRule.getPerKmAmountInCurrency() * Math.max(0,
        useDistanceKm - sectionDistancesKm.values().stream().mapToDouble(Double::doubleValue).sum());
    for (var sectionDistance : sectionDistancesKm.entrySet()) {
      startAmountInCurrency += priceRule.findSectionStartAmountInCurrency(sectionDistance.getKey());
      kmAmountInCurrency += priceRule.findPerKmAmountInCurrency(sectionDistance.getKey()) * sectionDistance.getValue();
    }
    return computeCumulativePriceFromAmounts(scheme, mode, pricing, distanceTravelledForBinSelectionKm, useDistanceKm,
        startAmountInCurrency, kmAmountInCurrency, capTotals);
  }

  /**
   * The cumulative price of one use of an entry-exit scheme, as the first use in every cap's period, see
   * {@link #computeCumulativePrice(EntryExitPricingScheme, Mode, TimeInWeekWithDayType, double, PricingSchemeGate,
   * PricingSchemeGate, PriceCapTotals)}
   *
   * @param scheme an entry-exit scheme registered with this network pricing
   * @param mode using it
   * @param moment the use starts at
   * @param distanceTravelledForBinSelectionKm the distance travelled on the scheme in its distance bands' period
   *     before the use, which picks the distance band
   * @param entryGate the use enters at
   * @param exitGate the use leaves at
   * @return what the use costs, 0 for a mode the scheme does not charge
   */
  public double computeCumulativePrice(
      EntryExitPricingScheme scheme,
      Mode mode,
      TimeInWeekWithDayType moment,
      double distanceTravelledForBinSelectionKm,
      PricingSchemeGate entryGate,
      PricingSchemeGate exitGate) {
    return computeCumulativePrice(
        scheme, mode, moment, distanceTravelledForBinSelectionKm, entryGate, exitGate, null);
  }

  /**
   * The cumulative price of one use of an entry-exit scheme: the amount of its pair of gates at the moment it starts,
   * times the factor of the distance band where it starts, then the cap per use, the minimum and the caps over a
   * period, see {@link #computeCumulativePriceFromAmounts}
   *
   * @param scheme an entry-exit scheme registered with this network pricing
   * @param mode using it
   * @param moment the use starts at
   * @param distanceTravelledForBinSelectionKm the distance travelled on the scheme in its distance bands' period
   *     before the use, which picks the distance band
   * @param entryGate the use enters at
   * @param exitGate the use leaves at
   * @param capTotals the traveller's totals per cap, null to count the use as the first in every cap's period
   * @return what the use costs, 0 for a mode the scheme does not charge
   */
  public double computeCumulativePrice(
      EntryExitPricingScheme scheme,
      Mode mode,
      TimeInWeekWithDayType moment,
      double distanceTravelledForBinSelectionKm,
      PricingSchemeGate entryGate,
      PricingSchemeGate exitGate,
      PriceCapTotals capTotals) {
    if (!scheme.chargesMode(mode)) {
      return 0;
    }
    var pricing = (SchemePricingImpl<FromToGatesPriceRule>) scheme.getPricing();
    double amountInCurrency = pricing.findPointPriceRuleInTimeBand(mode, pricing.findTimeBand(moment))
        .findAmountInCurrency(entryGate, exitGate);
    return computeCumulativePriceFromAmounts(
        scheme, mode, pricing, distanceTravelledForBinSelectionKm, 0, amountInCurrency, 0,
        capTotals);
  }

  /**
   * Log general information on this network pricing to the user
   *
   * @param prefix to use
   */
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s XML id %s (external id: %s)", prefix, getXmlId(), getExternalId()));
    LOGGER.info(String.format("%s #mode schemes: %d", prefix, modeSchemes.size()));
    LOGGER.info(String.format("%s #location schemes: %d", prefix, locationSchemes.size()));
    LOGGER.info(String.format("%s #entry-exit schemes: %d", prefix, entryExitSchemes.size()));
    LOGGER.info(String.format("%s #sections: %d, #gates: %d", prefix, sections.size(), gates.size()));
    LOGGER.info(String.format("%s #caps: %d", prefix, caps.size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkPricing shallowClone() {
    return new NetworkPricing(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkPricing deepClone() {
    return new NetworkPricing(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    caps.reset();
    modeSchemes.reset();
    locationSchemes.reset();
    entryExitSchemes.reset();
    sections.reset();
    gates.reset();
    IdGenerator.reset(pricingToken, PricingScheme.SCHEME_ID_CLASS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }
}
