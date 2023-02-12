package org.goplanit.demands;

import org.goplanit.userclass.TravellerType;
import org.goplanit.utils.id.*;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

import java.util.HashMap;

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
   */
  public TravellerTypes(TravellerTypes other, boolean deepCopy) {
    super(other, deepCopy);

    this.factory = new TravellerTypesFactory(other.getFactory().getIdGroupingToken(), this);
    if(deepCopy){
      this.clear();
      other.forEach( tt -> register(tt.deepClone()));
    }

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
    return findFirst(travelerType -> xmlId.equals(((TravellerType) travelerType).getXmlId()));
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
    return new TravellerTypes(this, false);
  }

  /**
   * Support deep clone --> once move to managed id this becomes mandatory override
   */
  @Override
  public TravellerTypes deepClone() {
    return new TravellerTypes(this, true);
  }

}
