package org.goplanit.demands.discrete.household;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory class for household instances to be registered on its parent container passed in to constructor
 */
public class HouseholdsFactory extends ManagedIdEntityFactoryImpl<Household>
    implements ManagedIdEntityFactory<Household> {

  /** container to use */
  protected final Households households;

  /**
   * Create a newly created instance without registering on the container
   *
   * @return created time period
   */
  protected Household createNew() {
    return new Household(getIdGroupingToken());
  }

  /**
   * Constructor
   *
   * @param tokenId    to use
   * @param households to use
   */
  protected HouseholdsFactory(final IdGroupingToken tokenId, final Households households) {
    super(tokenId);
    this.households = households;
  }

  /**
   * register a new entry on the container and return it
   *
   * @return created instance
   */
  public Household registerNew() {
    var newInstance = new Household(getIdGroupingToken());
    households.register(newInstance);
    return newInstance;
  }

}
