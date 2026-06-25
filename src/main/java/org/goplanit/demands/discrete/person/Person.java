package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.household.Household;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.OdZone;

import java.util.logging.Logger;

/**
 * Represents a person.
 * 
 * @author markr
 *
 */
public class Person extends ExternalIdAbleImpl implements ManagedId {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Person.class.getCanonicalName());

  /** household the person resides in */
  private Household household;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Person.PERSON_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<Person> PERSON_ID_CLASS = Person.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Person> getIdClass() {
    return PERSON_ID_CLASS;
  }

  /**
   * Constructor
   *
   * @param groupId          contiguous id generation within this group for instances of this class
   */
  public Person(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, PERSON_ID_CLASS));
  }

  /**
   * Copy constructor
   *
   * @param person to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Person(Person person, boolean deepCopy /* no impact yet */) {
    super(person);
    this.household = person.household;
  }

  /**
   * Access to household person resides in
   * @return household
   */
  public Household getHousehold() {
    return household;
  }

  /**
   * household person resides in
   * @param household to set
   */
  public void setHousehold(Household household) {
    this.household = household;
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
  public Person shallowClone() {
    return new Person(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Person deepClone() {
    return new Person(this, true);
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
