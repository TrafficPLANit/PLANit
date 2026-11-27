package org.goplanit.pricing.modifier;


import org.goplanit.pricing.PriceCapImpl;
import org.goplanit.pricing.PricingSchemeImpl;
import org.goplanit.pricing.NetworkPricing;
import org.goplanit.utils.pricing.modifier.event.RemovePricingSchemeEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.pricing.EntryExitPricingScheme;
import org.goplanit.utils.pricing.LocationPricingScheme;
import org.goplanit.utils.pricing.ModePricingScheme;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.pricing.modifier.NetworkPricingModifier;
import org.goplanit.utils.pricing.modifier.event.NetworkPricingModificationEvent;
import org.goplanit.utils.pricing.modifier.event.NetworkPricingModifierEventType;
import org.goplanit.utils.pricing.modifier.event.NetworkPricingModifierListener;
import org.goplanit.utils.pricing.modifier.event.NetworkPricingModifierEventProducer;

/**
 * Modifier of a network pricing: removes schemes, with what depends on them, and recreates scheme ids
 * <p>
 * A scheme is removed when the network takes away what it charges on, such as its last toll road. Its caps lose it, a
 * cap left covering no scheme goes too, and those listening hear of the removal. Removals leave gaps in the ids, which
 * recreating them closes, across the kinds and within each.
 * </p>
 *
 * @author markr
 */
public class NetworkPricingModifierImpl extends EventProducerImpl implements NetworkPricingModifier {

  /** the network pricing modified */
  private final NetworkPricing networkPricing;

  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((NetworkPricingModifierListener) eventListener).onNetworkPricingModifierEvent(
        (NetworkPricingModificationEvent) event);
  }

  /**
   * Constructor
   *
   * @param networkPricing to modify
   */
  public NetworkPricingModifierImpl(final NetworkPricing networkPricing) {
    super(NetworkPricingModifierEventProducer.FIRED_NETWORK_PRICING_EVENT_TYPES);
    this.networkPricing = networkPricing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removePricingScheme(PricingScheme scheme) {
    if (scheme instanceof ModePricingScheme) {
      networkPricing.getModeSchemes().remove((ModePricingScheme) scheme);
    } else if (scheme instanceof LocationPricingScheme) {
      networkPricing.getLocationSchemes().remove((LocationPricingScheme) scheme);
    } else if (scheme instanceof EntryExitPricingScheme) {
      networkPricing.getEntryExitSchemes().remove((EntryExitPricingScheme) scheme);
    }
    ((PricingSchemeImpl<?>) scheme).getSectionsOrNone().forEach(networkPricing.getSections()::remove);
    ((PricingSchemeImpl<?>) scheme).getGatesOrNone().forEach(networkPricing.getGates()::remove);
    var caps = networkPricing.getCaps();
    for (var cap : caps.getByScheme(scheme)) {
      caps.update(cap, c -> ((PriceCapImpl) c).removeScheme(scheme));
      if (cap.getSchemes().isEmpty()) {
        caps.remove(cap);
      }
    }
    if (hasListener(RemovePricingSchemeEvent.EVENT_TYPE)) {
      fireEvent(new RemovePricingSchemeEvent(this, scheme));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreatePricingSchemeIds() {
    IdGenerator.reset(networkPricing.getModeSchemes().getFactory().getIdGroupingToken(), PricingScheme.SCHEME_ID_CLASS);
    networkPricing.getModeSchemes().recreateIds(true);
    networkPricing.getLocationSchemes().recreateIds(true);
    networkPricing.getEntryExitSchemes().recreateIds(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(NetworkPricingModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(NetworkPricingModifierListener listener, NetworkPricingModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(NetworkPricingModifierListener listener, NetworkPricingModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(NetworkPricingModifierListener listener) {
    super.removeListener(listener);
  }
}
