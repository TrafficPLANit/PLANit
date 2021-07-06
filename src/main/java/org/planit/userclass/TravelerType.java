package org.planit.userclass;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Traveler type is a placeholder for all different types of traveler characteristics that affect the user class in the path choice component of traffic assignment. Together with
 * the mode this largely defines each user class TODO: Not used yet in UserClass class
 * 
 * @author markr
 *
 */
public class TravelerType extends ExternalIdAbleImpl {

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default XML id
   */
  public static final String DEFAULT_XML_ID = "1";

  /**
   * Name of this traveler type
   */
  private final String name;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TravelerType(final IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, TravelerType.class));
    this.name = DEFAULT_NAME;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TravelerType(final TravelerType other) {
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
  public TravelerType(final IdGroupingToken groupId, final String name) {
    super(IdGenerator.generateId(groupId, TravelerType.class));
    this.name = name;
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
  public TravelerType clone() {
    return new TravelerType(this);
  }
}
