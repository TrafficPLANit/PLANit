package org.goplanit.pricing;

import java.util.Collection;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeGateFactory;
import org.goplanit.utils.pricing.PricingSchemeGates;

/**
 * Factory for the gates of a network pricing, registering each on its container
 * <p>
 * Gates are registered before the entry-exit schemes using them, as a pricing file lists them; whether a gate's entries
 * and exits lie where its scheme's priced link segments are entered and left is checked when the scheme is registered.
 * </p>
 *
 * @author markr
 */
public class PricingSchemeGateFactoryImpl extends ManagedIdEntityFactoryImpl<PricingSchemeGate>
    implements PricingSchemeGateFactory {

  /** container to register gates on */
  private final PricingSchemeGates gates;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param gates to register on
   */
  protected PricingSchemeGateFactoryImpl(final IdGroupingToken groupId, final PricingSchemeGates gates) {
    super(groupId);
    this.gates = gates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGate registerNew(
      Collection<MacroscopicLinkSegment> entryLinkSegments,
      Collection<MacroscopicLinkSegment> exitLinkSegments) {
    var gate = new PricingSchemeGateImpl(getIdGroupingToken(), entryLinkSegments, exitLinkSegments);
    gates.register(gate);
    return gate;
  }
}
