package org.goplanit.userclass;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.StringUtils;

/**
 * Traveller type is a placeholder for all different types of traveler characteristics that affect the user class in the path choice component of traffic assignment. Together with
 * the mode this largely defines each user class TODO: Not used yet in UserClass class
 * 
 * @author markr
 *
 */
public class TravellerType extends ExternalIdAbleImpl {

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default XML id
   */
  public static final String DEFAULT_XML_ID = "1";

  /**
   * Name of this traveller type
   */
  private final String name;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TravellerType(final IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, TravellerType.class));
    this.name = DEFAULT_NAME;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TravellerType(final TravellerType other) {
    super(other);
    this.name = other.name;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name    name of this traveler type
   * 
   */
  public TravellerType(final IdGroupingToken groupId, final String name) {
    super(IdGenerator.generateId(groupId, TravellerType.class));
    this.name = name;
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
   * Collect the name
   * 
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TravellerType clone() {
    return new TravellerType(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("[id: %d, XMLid: %s, name: %s]", getId(), getXmlId(), getName());
  }
}
