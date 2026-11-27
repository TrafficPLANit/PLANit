package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.PricingScheme;

/**
 * A cap on what one or more schemes charge over a period: either the most paid, or a number of uses after which they
 * charge nothing
 * <p>
 * For example a weekly cap on a city's toll roads, or a cordon charged once a day. A cap that makes no sense on its
 * own, covering no scheme, giving both an amount and a count, or counting per use, is refused when made; with neither
 * an amount nor a count it allows one charged use per period. Caps are not in time bands and factors do not scale them.
 * </p>
 *
 * @author markr
 */
public class PriceCapImpl extends ExternalIdAbleImpl implements PriceCap {

  /** generated UID */
  private static final long serialVersionUID = 2954066180371192247L;

  /** the schemes covered */
  private final List<PricingScheme> schemes;

  /** the most paid over the period, null when the cap counts uses */
  private final Double amountInCurrency;

  /** the number of uses after which the schemes charge nothing, null when the cap gives an amount */
  private final Integer activateAboveCount;

  /** the period counted over */
  private final PricingPeriod period;

  /** the modes capped */
  private final PricedModeSelection pricedModeSelection;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param schemes covered, at least one
   * @param amountInCurrency the most paid over the period, null when the cap counts uses
   * @param activateAboveCount the number of uses in the period that are charged, the later ones free (3: the fourth use
   *     and later are free), null when the cap gives an amount; with neither, 1
   * @param period counted over
   * @param pricedModeSelection the modes capped
   * @throws IllegalArgumentException when it covers no scheme, gives both an amount and a count, or counts per use
   */
  protected PriceCapImpl(
      IdGroupingToken groupId,
      Collection<? extends PricingScheme> schemes,
      Double amountInCurrency,
      Integer activateAboveCount,
      PricingPeriod period,
      PricedModeSelection pricedModeSelection) {
    super(IdGenerator.generateId(groupId, CAP_ID_CLASS));
    ExceptionUtils.throwIllegalArgumentIf(schemes.isEmpty(), "Cap refused, it covers no scheme");
    ExceptionUtils.throwIllegalArgumentIf(amountInCurrency != null && activateAboveCount != null,
        "Cap refused, it gives either an amount or a count of uses, not both");
    ExceptionUtils.throwIllegalArgumentIf(amountInCurrency == null && period == PricingPeriod.USE,
        "Cap refused, a count of uses is per hour, day, week or month, not per use");
    this.schemes = new ArrayList<>(schemes);
    this.amountInCurrency = amountInCurrency;
    this.activateAboveCount =
        amountInCurrency == null && activateAboveCount == null ? Integer.valueOf(1) : activateAboveCount;
    this.period = period;
    this.pricedModeSelection = pricedModeSelection;
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own list, referring to the same schemes; pointing those
   * to copied schemes is up to the copy of the pricing
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PriceCapImpl(PriceCapImpl other, boolean deepCopy) {
    super(other);
    this.schemes = new ArrayList<>(other.schemes);
    this.amountInCurrency = other.amountInCurrency;
    this.activateAboveCount = other.activateAboveCount;
    this.period = other.period;
    this.pricedModeSelection = other.pricedModeSelection;
  }

  /**
   * Point the schemes covered to others, such as their copies in a copy of the pricing
   *
   * @param schemeMapping from each scheme to the one to cover
   */
  protected void updateSchemeMapping(UnaryOperator<PricingScheme> schemeMapping) {
    schemes.replaceAll(schemeMapping);
  }

  /**
   * Remove a scheme from those covered, such as one removed from the pricing
   *
   * @param scheme to remove
   * @return true when the cap covered it
   */
  public boolean removeScheme(PricingScheme scheme) {
    return schemes.removeIf(held -> held == scheme);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingScheme> getSchemes() {
    return Collections.unmodifiableList(schemes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getAmountInCurrency() {
    return amountInCurrency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getActivateAboveCount() {
    return activateAboveCount;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingPeriod getPeriod() {
    return period;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricedModeSelection getPricedModeSelection() {
    return pricedModeSelection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setId(IdGenerator.generateId(tokenId, getIdClass()));
    return getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapImpl shallowClone() {
    return new PriceCapImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PriceCapImpl deepClone() {
    return new PriceCapImpl(this, true);
  }
}
