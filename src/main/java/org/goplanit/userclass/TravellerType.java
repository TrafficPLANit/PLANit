package org.goplanit.userclass;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.time.TimePeriod;

/**
 * Traveller type is a placeholder for all different types of traveler characteristics that affect the user class in the path choice component of traffic assignment. Together with
 * the mode this largely defines each user class TODO: Not used yet in UserClass class
 * 
 * @author markr
 *
 */
public class TravellerType extends ExternalIdAbleImpl implements ManagedId {

  /**
   * Name of this traveller type
   */
  private final String name;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, TravellerType.TRAVELLERTYPE_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<TravellerType> TRAVELLERTYPE_ID_CLASS = TravellerType.class;

  /**
   * default name
   */
  public static final String DEFAULT_NAME = "Default";

  /**
   * Default XML id
   */
  public static final String DEFAULT_XML_ID = "1";

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
  public Class<? extends TravellerType> getIdClass() {
    return TRAVELLERTYPE_ID_CLASS;
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
  public TravellerType shallowClone() {
    return new TravellerType(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TravellerType deepClone() {
    return shallowClone(); // no impact
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("[id: %d, XMLid: %s, name: %s]", getId(), getXmlId(), getName());
  }
}
