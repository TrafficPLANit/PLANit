package org.goplanit.demands.discrete.household;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

import java.util.function.BiConsumer;

/**
 * Class to register and store households for the current discrete demand object
 *
 * @author garym, markr
 */
public final class Households extends ManagedIdEntitiesImpl<Household> {

  /** factory to create instances on this container */
  private final HouseholdsFactory factory;

  /**
   * Constructor
   *
   * @param tokenId  to use for id generation
   */
  public Households(final IdGroupingToken tokenId) {
    super(Household::getId, Household.HOUSEHOLD_ID_CLASS);
    this.factory = new HouseholdsFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public Households(Households other, boolean deepCopy, BiConsumer<Household, Household> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new HouseholdsFactory(other.getFactory().getIdGroupingToken(), this);
  }


  /**
   * Retrieve by its xml Id
   * <p>
   * This method is not efficient, since it loops through all the registered entries in order to find the
   * required one.
   *
   * @param xmlId the XML Id of the entity
   * @return the retrieved entity, or null if nothing was found
   */
  public Household getByXmlId(final String xmlId) {
    return firstMatch(timePeriod -> xmlId.equals(timePeriod.getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HouseholdsFactory getFactory() {
    return this.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Households shallowClone() {
    return new Households(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  public Households deepClone() {
    return new Households(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Households deepCloneWithMapping(BiConsumer<Household, Household> mapper) {
    return new Households(this, true, mapper);
  }
}
