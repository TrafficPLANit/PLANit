package org.goplanit.pricing;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.pricing.PriceCapFactory;
import org.goplanit.utils.pricing.PriceCaps;
import org.goplanit.utils.pricing.PriceCapTotals;
import org.goplanit.utils.pricing.PricingScheme;

/**
 * Container for the caps of a network pricing, those of one scheme and those covering several, with a lookup of the
 * caps covering a scheme
 * <p>
 * Where a cap sits in the file, in its scheme or at the top, follows from how many schemes it covers, so all caps are
 * held here alike. A scheme's caps are found through the lookup, for example when the scheme is removed and its caps
 * lose it.
 * </p>
 *
 * @author markr
 */
public class PriceCapsImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<PriceCap> implements PriceCaps {

  /** factory to create caps with */
  private final PriceCapFactoryImpl factory;

  /** scheme to the caps covering it, by identity */
  private final EntityIndex<PricingScheme, PriceCap> byScheme = addForeignKeyIndex(PriceCap::getSchemes);

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  protected PriceCapsImpl(final IdGroupingToken groupId) {
    super(PriceCap::getId, PriceCap.CAP_ID_CLASS);
    this.factory = new PriceCapFactoryImpl(groupId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy, may be null
   */
  protected PriceCapsImpl(final PriceCapsImpl other, boolean deepCopy, BiConsumer<PriceCap, PriceCap> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new PriceCapFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * Point the schemes every cap covers to others, such as their copies in a copy of the pricing; the index is rebuilt
   * to match
   *
   * @param schemeMapping from each scheme to the one to cover
   */
  protected void updateSchemeMapping(UnaryOperator<PricingScheme> schemeMapping) {
    forEach(cap -> ((PriceCapImpl) cap).updateSchemeMapping(schemeMapping));
    reindex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PriceCap> getByScheme(PricingScheme scheme) {
    return byScheme.get(scheme);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapTotals createPriceCapTotals() {
    return new PriceCapTotalsImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapsImpl shallowClone() {
    return new PriceCapsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapsImpl deepClone() {
    return new PriceCapsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapsImpl deepCloneWithMapping(BiConsumer<PriceCap, PriceCap> mapper) {
    return new PriceCapsImpl(this, true, mapper);
  }
}
