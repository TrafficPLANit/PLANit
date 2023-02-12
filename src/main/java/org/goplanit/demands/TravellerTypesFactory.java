package org.goplanit.demands;

import org.goplanit.time.TimePeriodImpl;
import org.goplanit.userclass.TravellerType;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.time.TimePeriod;

/**
 * Factory class for TravellerType instances to be registered on its parent container passed in to constructor
 */
public class TravellerTypesFactory extends ManagedIdEntityFactoryImpl<TravellerType> implements ManagedIdEntityFactory<TravellerType> {

  /** container to use */
  protected final TravellerTypes travellerTypes;

  /**
   * Create a newly created instance without registering on the container
   *
   * @param name name of the traveller type
   * @return created traveller type
   */
  protected TravellerType createNew(String name) {
    return new TravellerType(getIdGroupingToken(), name);
  }

  /**
   * Constructor
   *
   * @param tokenId              to use
   * @param travellerTypes to use
   */
  protected TravellerTypesFactory(final IdGroupingToken tokenId, final TravellerTypes travellerTypes) {
    super(tokenId);
    this.travellerTypes = travellerTypes;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param name for the traveller type
   * @return created traveller type
   */
  public TravellerType registerNew(String name) {
    TravellerType newInstance = createNew(name);
    travellerTypes.register(newInstance);
    return newInstance;
  }

}
