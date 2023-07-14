package org.goplanit.demands;

import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.mode.Mode;

/**
 * Factory class for user classes instances to be registered on its parent container passed in to constructor
 */
public class UserClassesFactory extends ManagedIdEntityFactoryImpl<UserClass> implements ManagedIdEntityFactory<UserClass> {

  /** container to use */
  protected final UserClasses userClasses;

  /**
   * Create a newly created instance without registering on the container
   *
   * @param name name of the user class
   * @param mode mode of the user class
   * @param travelerType of the user class
   * @return created user class
   */
  protected UserClass createNew(final String name, final Mode mode, final TravellerType travelerType) {
    return new UserClass(getIdGroupingToken(), name, mode, travelerType);
  }

  /**
   * Constructor
   *
   * @param tokenId              to use
   * @param userClasses to use
   */
  protected UserClassesFactory(final IdGroupingToken tokenId, final UserClasses userClasses) {
    super(tokenId);
    this.userClasses = userClasses;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param name name of the user class
   * @param mode mode of the user class
   * @param travelerType of the user class
   * @return created user class
   */
  public UserClass registerNew(final String name, final Mode mode, final TravellerType travelerType) {
    UserClass newInstance = createNew(name, mode, travelerType);
    userClasses.register(newInstance);
    return newInstance;
  }

}
