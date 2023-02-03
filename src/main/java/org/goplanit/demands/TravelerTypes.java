package org.goplanit.demands;

import org.goplanit.userclass.TravellerType;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

import java.util.HashMap;

/**
 * Inner class to register and store traveler types for the current demand object
 *
 * @author markr
 */
public class TravelerTypes extends LongMapWrapperImpl<TravellerType> {

  private final Demands demands;

  /**
   * Constructor
   */
  public TravelerTypes(Demands demands) {
    super(new HashMap<>(), TravellerType::getId);
    this.demands = demands;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public TravelerTypes(TravelerTypes other, boolean deepCopy) {
    super(other);

    this.demands = other.demands;
    if(deepCopy){
      this.clear();
      other.forEach( tt -> register(tt.deepClone()));
    }

  }

  /**
   * Factory method to create and register a new travel type on the demands
   *
   * @param name the name of the travel type
   * @return new traveler type created
   */
  public TravellerType createAndRegisterNew(String name) {
    TravellerType newTravelerType = new TravellerType(demands.getIdGroupingToken(), name);
    register(newTravelerType);
    return newTravelerType;
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
  public TravelerTypes shallowClone() {
    return new TravelerTypes(this, false);
  }

  /**
   * Support deep clone --> once move to managed id this becomes mandatory override
   */
  public TravelerTypes deepClone() {
    return new TravelerTypes(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    super.clear();
    IdGenerator.reset(demands.getIdGroupingToken(), TravellerType.class);
  }
}
