package org.planit.userclass;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Traveler type is a placeholder for all different types of traveler characteristics that affect the user class in the route choice component of traffic assignment. Together with
 * the mode this largely defines each user class TODO: Not used yet in UserClass class
 * 
 * @author markr
 *
 */
public class TravelerType {

  public static final String DEFAULT_NAME = "Default";
  /**
   * If no user class is defined the default user class will be assumed to have a traveler type referencing the default external traveler type id (1)
   */
  public static final long DEFAULT_EXTERNAL_ID = 1;

  /**
   * Unique feature id
   */
  private final long id;

  /**
   * Unique external id
   */
  private final Object externalId;

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
    this.id = IdGenerator.generateId(groupId, TravelerType.class);
    this.name = DEFAULT_NAME;
    this.externalId = DEFAULT_EXTERNAL_ID;
  }

  /**
   * Constructor
   * 
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param externaId external id of this traveler type
   * @param name      name of this traveler type
   * 
   */
  public TravelerType(final IdGroupingToken groupId, final Object externaId, final String name) {
    this.id = IdGenerator.generateId(groupId, TravelerType.class);
    this.externalId = externaId;
    this.name = name;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name    name of this traveler type
   */
  public TravelerType(final IdGroupingToken groupId, final String name) {
    this.id = IdGenerator.generateId(groupId, TravelerType.class);
    this.externalId = DEFAULT_EXTERNAL_ID;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Object getExternalId() {
    return externalId;
  }

  public boolean hasExternalId() {
    return (externalId != null);
  }
}
