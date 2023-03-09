package org.goplanit.demands;

import org.goplanit.userclass.UserClass;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.time.TimePeriod;

import java.util.function.BiConsumer;

/**
 * Inner class to register and store user classes for the current demand object
 *
 * @author markr
 */
public class UserClasses extends ManagedIdEntitiesImpl<UserClass> {

  /** factory to create instances on this container */
  private final UserClassesFactory factory;

  /**
   * Constructor
   *
   * @param tokenId to use
   */
  public UserClasses(final IdGroupingToken tokenId) {
    super(UserClass::getId, UserClass.USERCLASS_ID_CLASS);
    this.factory = new UserClassesFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public UserClasses(UserClasses other, boolean deepCopy, BiConsumer<UserClass, UserClass> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new UserClassesFactory(other.getFactory().getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserClassesFactory getFactory() {
    return factory;
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
    return firstMatch(userClass -> xmlId.equals(((UserClass) userClass).getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserClasses shallowClone() {
    return new UserClasses(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  public UserClasses deepClone() {
    return new UserClasses(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserClasses deepCloneWithMapping(BiConsumer<UserClass, UserClass> mapper) {
    return new UserClasses(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    super.clear();
  }
}
