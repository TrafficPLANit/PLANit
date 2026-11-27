package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.pricing.PriceCapFactory;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.PricingScheme;

/**
 * Factory for caps, registering each on its container
 * <p>
 * It makes and registers what it is given; the caller checks first, through {@link PriceCapUtils}: the modes a cap
 * names limited to those its schemes charge, and a cap below a minimum refused, so any caller can ask the same
 * questions.
 * </p>
 *
 * @author markr
 */
public class PriceCapFactoryImpl extends ManagedIdEntityFactoryImpl<PriceCap> implements PriceCapFactory {

  /** container to register on */
  private final PriceCapsImpl caps;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param caps to register on
   */
  protected PriceCapFactoryImpl(final IdGroupingToken groupId, final PriceCapsImpl caps) {
    super(groupId);
    this.caps = caps;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCap registerNew(
      List<PricingScheme> schemes,
      Double amountInCurrency,
      Integer activateAboveCount,
      PricingPeriod period,
      PricedModeSelection modes) {
    var cap = new PriceCapImpl(getIdGroupingToken(), schemes, amountInCurrency, activateAboveCount, period, modes);
    caps.register(cap);
    return cap;
  }
}
