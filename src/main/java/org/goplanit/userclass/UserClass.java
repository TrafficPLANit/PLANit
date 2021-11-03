package org.goplanit.userclass;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;

/**
 * A user class defines a combination of one or more characteristics of users in an aggregate representation of traffic which partially dictate how they behave in traffic
 * assignment.
 *
 * @author markr
 *
 */
public class UserClass extends ExternalIdAbleImpl {

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default XML id
   */
  public static final String DEFAULT_XML_ID = "1";

  /**
   * Name of this user class
   */
  private final String name;

  /**
   * Mode of travel of this user class
   */
  private final Mode mode;

  /**
   * Traveler type of this user class
   */
  private final TravelerType travellerType;

  /**
   * Constructor of user class
   *
   * @param groupId      contiguous id generation within this group for instances of this class
   * @param name         name of this user class
   * @param mode         the mode of travel
   * @param travelerType the travelerType
   */
  public UserClass(final IdGroupingToken groupId, final String name, final Mode mode, final TravelerType travelerType) {
    super(IdGenerator.generateId(groupId, UserClass.class));
    this.name = name;
    this.travellerType = travelerType;
    this.mode = mode;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public UserClass(final UserClass other) {
    super(other);
    this.name = other.name;
    this.travellerType = other.travellerType;
    this.mode = other.mode;
  }

  /**
   * Get the traveler type of this user class
   *
   * @return TravelerType of this user class
   */
  public TravelerType getTravelerType() {
    return travellerType;
  }

  /**
   * Get the name of this user class
   *
   * @return the name of this user class
   */
  public String getName() {
    return name;
  }

  /**
   * Return the mode of travel of this user class
   *
   * @return Mode of this user class
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserClass clone() {
    return new UserClass(this);
  }

}
