package org.planit.userclass;

import org.planit.utils.id.ExternalIdAble;
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
public class UserClass implements ExternalIdAble {

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default external id
   */
  public static final String DEFAULT_XML_ID = "1";

  /**
   * id of this user class
   */
  private final long id;

  /**
   * External Id of this user class
   */
  private String externalId;

  /**
   * xml Id
   */
  private String xmlId;

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
    this.id = IdGenerator.generateId(groupId, UserClass.class);
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
  @Override
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
   * {@inheritDoc}
   */
  @Override
  public String getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return this.xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

}
