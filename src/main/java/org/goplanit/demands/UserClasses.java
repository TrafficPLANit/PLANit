package org.goplanit.demands;

import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

import java.util.HashMap;

/**
 * Inner class to register and store user classes for the current demand object
 *
 * @author markr
 */
public class UserClasses extends LongMapWrapperImpl<UserClass> {

  private final Demands demands;

  /**
   * Constructor
   */
  public UserClasses(Demands demands) {
    super(new HashMap<>(), UserClass::getId);
    this.demands = demands;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public UserClasses(UserClasses other, boolean deepCopy) {
    this(other.demands);

    if(deepCopy){
      this.clear();
      other.forEach( uc -> register(uc.deepClone()));
    }

  }

  /**
   * Factory method to create and register a new user class on the demands
   *
   * @param name          the name for this user class
   * @param mode          the mode for this user class
   * @param travellerType the travel type for this user class
   * @return new traveler type created
   */
  public UserClass createAndRegister(String name, Mode mode, TravellerType travellerType) {
    var newUserClass = new UserClass(demands.getIdGroupingToken(), name, mode, travellerType);
    register(newUserClass);
    return newUserClass;
  }

  /**
   * Retrieve a UserClass by its XML Id
   * <p>
   * This method is not efficient, since it loops through all the registered user classes in order to find the required entry.
   *
   * @param xmlId the XML Id of the specified user class
   * @return the retrieved user class, or null if no user class was found
   */
  public UserClass getUserClassByXmlId(String xmlId) {
    return findFirst(userClass -> xmlId.equals(((UserClass) userClass).getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserClasses shallowClone() {
    return new UserClasses(this, false);
  }

  /**
   * Support deep clone --> once move to managed id this becomes mandatory override
   */
  public UserClasses deepClone() {
    return new UserClasses(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    super.clear();
    IdGenerator.reset(demands.getIdGroupingToken(), UserClass.class);
  }
}
