package org.goplanit.pricing;

import java.util.function.BiConsumer;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.pricing.ModePricingScheme;
import org.goplanit.utils.pricing.ModePricingSchemeFactory;
import org.goplanit.utils.pricing.ModePricingSchemes;

/**
 * Container for the mode schemes of a network pricing, keyed by their id across all schemes, with a lookup of the mode
 * scheme charging a mode
 * <p>
 * Each mode is charged by at most one mode scheme, so a new mode scheme is checked against this lookup, one look per
 * mode it charges, rather than against every mode scheme.
 * </p>
 *
 * @author markr
 */
public class ModePricingSchemesImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<ModePricingScheme>
    implements ModePricingSchemes {

  /** factory to create mode schemes with */
  private final ModePricingSchemeFactoryImpl factory;

  /** mode to the mode scheme charging it, by identity */
  private final EntityIndex<Mode, ModePricingScheme> byChargedMode =
      addForeignKeyIndex(ModePricingScheme::getChargedModes);

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param networkPricing the schemes belong to
   */
  protected ModePricingSchemesImpl(final IdGroupingToken groupId, final NetworkPricing networkPricing) {
    super(ModePricingScheme::getId, ModePricingScheme.MODE_SCHEME_ID_CLASS);
    this.factory = new ModePricingSchemeFactoryImpl(groupId, networkPricing, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy, may be null
   * @param networkPricing the copies belong to
   */
  protected ModePricingSchemesImpl(
      final ModePricingSchemesImpl other,
      boolean deepCopy,
      BiConsumer<ModePricingScheme, ModePricingScheme> mapper,
      final NetworkPricing networkPricing) {
    super(other, deepCopy, mapper);
    this.factory = new ModePricingSchemeFactoryImpl(
        other.factory.getIdGroupingToken(), networkPricing, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingScheme getByPricedMode(Mode mode) {
    return byChargedMode.getFirst(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemesImpl shallowClone() {
    return new ModePricingSchemesImpl(this, false, null, factory.getNetworkPricing());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemesImpl deepClone() {
    return new ModePricingSchemesImpl(this, true, null, factory.getNetworkPricing());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModePricingSchemesImpl deepCloneWithMapping(BiConsumer<ModePricingScheme, ModePricingScheme> mapper) {
    return new ModePricingSchemesImpl(this, true, mapper, factory.getNetworkPricing());
  }
}
