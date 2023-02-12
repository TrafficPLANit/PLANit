package org.goplanit.userclass;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.hsqldb.rights.User;

/**
 * A user class defines a combination of one or more characteristics of users in an aggregate representation of traffic which partially dictate how they behave in traffic
 * assignment.
 *
 * @author markr
 *
 */
public class UserClass extends ExternalIdAbleImpl implements ManagedId {

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
  private final TravellerType travellerType;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, UserClass.USERCLASS_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<UserClass> USERCLASS_ID_CLASS = UserClass.class;

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default XML id
   */
  public static final String DEFAULT_XML_ID = "1";

  /**
   * Constructor of user class
   *
   * @param groupId      contiguous id generation within this group for instances of this class
   * @param name         name of this user class
   * @param mode         the mode of travel
   * @param travelerType the travelerType
   */
  public UserClass(final IdGroupingToken groupId, final String name, final Mode mode, final TravellerType travelerType) {
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
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UserClass> getIdClass() {
    return USERCLASS_ID_CLASS;
  }

  /**
   * Get the traveler type of this user class
   *
   * @return TravelerType of this user class
   */
  public TravellerType getTravelerType() {
    return travellerType;
  }

  /**
   * check if it has a name
   * 
   * @return true when name is present false otherwise
   */
  public boolean hasName() {
    return !StringUtils.isNullOrBlank(name);
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
  public UserClass shallowClone() {
    return new UserClass(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserClass deepClone() {
    return shallowClone(); // no impact;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("[id: %d, XMLid: %s, name: %s, mode %s, travellertype %s]", getId(), getXmlId(), getName(), getMode().toString(), travellerType.toString());
  }
}
