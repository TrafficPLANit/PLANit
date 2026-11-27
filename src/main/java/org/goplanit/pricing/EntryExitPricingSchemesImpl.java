package org.goplanit.pricing;

import java.util.function.BiConsumer;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.pricing.EntryExitPricingScheme;
import org.goplanit.utils.pricing.EntryExitPricingSchemeFactory;
import org.goplanit.utils.pricing.EntryExitPricingSchemes;

/**
 * Container for the entry-exit schemes of a network pricing, keyed by their id across all schemes
 * <p>
 * One container per kind, as the pricing file has, so each kind is registered through a factory of its own taking
 * what that kind is made of.
 * </p>
 *
 * @author markr
 */
public class EntryExitPricingSchemesImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<EntryExitPricingScheme>
    implements EntryExitPricingSchemes {

  /** factory to create entry-exit schemes with */
  private final EntryExitPricingSchemeFactoryImpl factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param networkPricing the schemes belong to
   */
  protected EntryExitPricingSchemesImpl(final IdGroupingToken groupId, final NetworkPricing networkPricing) {
    super(EntryExitPricingScheme::getId, EntryExitPricingScheme.ENTRY_EXIT_SCHEME_ID_CLASS);
    this.factory = new EntryExitPricingSchemeFactoryImpl(groupId, networkPricing, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy, may be null
   * @param networkPricing the copies belong to
   */
  protected EntryExitPricingSchemesImpl(
      final EntryExitPricingSchemesImpl other,
      boolean deepCopy,
      BiConsumer<EntryExitPricingScheme, EntryExitPricingScheme> mapper,
      final NetworkPricing networkPricing) {
    super(other, deepCopy, mapper);
    this.factory = new EntryExitPricingSchemeFactoryImpl(
        other.factory.getIdGroupingToken(), networkPricing, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EntryExitPricingSchemeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EntryExitPricingSchemesImpl shallowClone() {
    return new EntryExitPricingSchemesImpl(this, false, null, factory.getNetworkPricing());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EntryExitPricingSchemesImpl deepClone() {
    return new EntryExitPricingSchemesImpl(this, true, null, factory.getNetworkPricing());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EntryExitPricingSchemesImpl deepCloneWithMapping(
      BiConsumer<EntryExitPricingScheme, EntryExitPricingScheme> mapper) {
    return new EntryExitPricingSchemesImpl(this, true, mapper, factory.getNetworkPricing());
  }
}
