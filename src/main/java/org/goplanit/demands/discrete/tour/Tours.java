package org.goplanit.demands.discrete.tour;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

import java.util.function.BiConsumer;

/**
 * Class to register and store tours for the current discrete demand object
 *
 * @author garym, markr
 */
public final class Tours extends ManagedIdEntitiesImpl<Tour> {

  /** factory to create instances on this container */
  private final ToursFactory factory;

  /**
   * Constructor
   *
   * @param tokenId  to use for id generation
   */
  public Tours(final IdGroupingToken tokenId) {
    super(Tour::getId, Tour.TOUR_ID_CLASS);
    this.factory = new ToursFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public Tours(Tours other, boolean deepCopy, BiConsumer<Tour, Tour> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new ToursFactory(other.getFactory().getIdGroupingToken(), this);
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
  public Tour getByXmlId(final String xmlId) {
    return firstMatch(tour -> xmlId.equals(tour.getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ToursFactory getFactory() {
    return this.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Tours shallowClone() {
    return new Tours(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  public Tours deepClone() {
    return new Tours(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Tours deepCloneWithMapping(BiConsumer<Tour, Tour> mapper) {
    return new Tours(this, true, mapper);
  }
}
