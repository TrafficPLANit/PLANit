package org.planit.userclass;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * A user class defines a combination of one or more characteristics of users in an aggregate representation of traffic which partially dictate how they behave in traffic
 * assignment.
 *
 * @author markr
 *
 */
public class UserClass {

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default external id
   */
  public static final long DEFAULT_EXTERNAL_ID = 1;

  /**
   * id of this user class
   */
  private final long id;

  /**
   * External Id of this user class
   */
  private Object externalId;

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
   * @param externalId   id of this user class
   * @param name         name of this user class
   * @param mode         the mode of travel
   * @param travelerType the travelerType
   */
  public UserClass(final IdGroupingToken groupId, final Object externalId, final String name, final Mode mode, final TravelerType travelerType) {
    this.id = IdGenerator.generateId(groupId, UserClass.class);
    this.externalId = externalId;
    this.name = name;
    this.travellerType = travelerType;
    this.mode = mode;
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
   * Get the id of this user class
   *
   * @return id of this user class
   */
  public long getId() {
    return id;
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
   * Get the external id of this user class
   *
   * @return external id of this user class
   */
  public Object getExternalId() {
    return externalId;
  }

  public boolean hasExternalId() {
    return (externalId != null);
  }

}
