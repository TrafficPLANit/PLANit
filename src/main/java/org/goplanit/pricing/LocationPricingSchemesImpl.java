package org.goplanit.pricing;

import java.util.function.BiConsumer;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.pricing.LocationPricingScheme;
import org.goplanit.utils.pricing.LocationPricingSchemeFactory;
import org.goplanit.utils.pricing.LocationPricingSchemes;

/**
 * Container for the location schemes of a network pricing, keyed by their id across all schemes
 * <p>
 * One container per kind, as the pricing file has, so each kind is registered through a factory of its own taking
 * what that kind is made of.
 * </p>
 *
 * @author markr
 */
public class LocationPricingSchemesImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<LocationPricingScheme>
    implements LocationPricingSchemes {

  /** factory to create location schemes with */
  private final LocationPricingSchemeFactoryImpl factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param networkPricing the schemes belong to
   */
  protected LocationPricingSchemesImpl(final IdGroupingToken groupId, final NetworkPricing networkPricing) {
    super(LocationPricingScheme::getId, LocationPricingScheme.LOCATION_SCHEME_ID_CLASS);
    this.factory = new LocationPricingSchemeFactoryImpl(groupId, networkPricing, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy, may be null
   * @param networkPricing the copies belong to
   */
  protected LocationPricingSchemesImpl(
      final LocationPricingSchemesImpl other,
      boolean deepCopy,
      BiConsumer<LocationPricingScheme, LocationPricingScheme> mapper,
      final NetworkPricing networkPricing) {
    super(other, deepCopy, mapper);
    this.factory = new LocationPricingSchemeFactoryImpl(
        other.factory.getIdGroupingToken(), networkPricing, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocationPricingSchemeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocationPricingSchemesImpl shallowClone() {
    return new LocationPricingSchemesImpl(this, false, null, factory.getNetworkPricing());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocationPricingSchemesImpl deepClone() {
    return new LocationPricingSchemesImpl(this, true, null, factory.getNetworkPricing());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocationPricingSchemesImpl deepCloneWithMapping(
      BiConsumer<LocationPricingScheme, LocationPricingScheme> mapper) {
    return new LocationPricingSchemesImpl(this, true, mapper, factory.getNetworkPricing());
  }
}
