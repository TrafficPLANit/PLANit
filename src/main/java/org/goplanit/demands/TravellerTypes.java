package org.goplanit.demands;

import org.goplanit.userclass.TravellerType;
import org.goplanit.utils.id.*;
import org.goplanit.utils.time.TimePeriod;

import java.util.function.BiConsumer;

/**
 * Inner class to register and store traveler types
 *
 * @author markr
 */
public class TravellerTypes extends ManagedIdEntitiesImpl<TravellerType> implements ManagedIdEntities<TravellerType> {

  /** factory to create instances on this container */
  private final TravellerTypesFactory factory;

  /**
   * Constructor
   *
   * @param tokenId to use
   */
  public TravellerTypes(final IdGroupingToken tokenId) {
    super(TravellerType::getId, TravellerType.TRAVELLERTYPE_ID_CLASS);
    this.factory = new TravellerTypesFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public TravellerTypes(TravellerTypes other, boolean deepCopy, BiConsumer<TravellerType, TravellerType> mapper) {
    super(other, deepCopy, mapper);

    this.factory = new TravellerTypesFactory(other.getFactory().getIdGroupingToken(), this);
  }

  /**
   * Retrieve a TravelerType by its XML Id
   * <p>
   * This method is not efficient, since it loops through all the registered traveler type in order to find the required entry.
   *
   * @param xmlId the XML Id of the specified traveler type
   * @return the retrieved traveler type, or null if no traveler type was found
   */
  public TravellerType getByXmlId(String xmlId) {
    return firstMatch(travelerType -> xmlId.equals(((TravellerType) travelerType).getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TravellerTypesFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TravellerTypes shallowClone() {
    return new TravellerTypes(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TravellerTypes deepClone() {
    return new TravellerTypes(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TravellerTypes deepCloneWithMapping(BiConsumer<TravellerType, TravellerType> mapper) {
    return new TravellerTypes(this, true, mapper);
  }

}
