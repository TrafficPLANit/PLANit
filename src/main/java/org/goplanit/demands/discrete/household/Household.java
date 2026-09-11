package org.goplanit.demands.discrete.household;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.OdZone;

import java.util.logging.Logger;

/**
 * Represents a household.
 * 
 * @author markr
 *
 */
public class Household extends ExternalIdAbleImpl implements ManagedId {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Household.class.getCanonicalName());

  /** zone the household resides in */
  private OdZone zone;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Household.HOUSEHOLD_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<Household> HOUSEHOLD_ID_CLASS = Household.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Household> getIdClass() {
    return HOUSEHOLD_ID_CLASS;
  }

  /**
   * Constructor
   *
   * @param groupId          contiguous id generation within this group for instances of this class
   */
  public Household(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, HOUSEHOLD_ID_CLASS));
  }

  /**
   * Copy constructor
   *
   * @param household to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Household(Household household, boolean deepCopy /* no impact yet */) {
    super(household);
    this.zone = household.zone;
  }

  /**
   * Access to zone household resides in
   * @return zone
   */
  public OdZone getZone() {
    return zone;
  }

  /**
   * zone household resides in
   * @param zone to set
   */
  public void setZone(OdZone zone) {
    this.zone = zone;
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
  public Household shallowClone() {
    return new Household(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Household deepClone() {
    return new Household(this, true);
  }

  /**
   * Output this object as a String
   * 
   * @return String containing the value of this
   */
  @Override
  public String toString() {
    return getIdsAsString();
  }

}
