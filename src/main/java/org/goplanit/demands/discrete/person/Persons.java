package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Class to register and store persons for the current discrete demand object
 *
 * @author garym, markr
 */
public final class Persons extends ManagedIdEntitiesImpl<Person> {

  /** factory to create instances on this container */
  private final PersonsFactory factory;

  /**
   * Constructor
   *
   * @param tokenId  to use for id generation
   */
  public Persons(final IdGroupingToken tokenId) {
    super(Person::getId, Person.PERSON_ID_CLASS);
    this.factory = new PersonsFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public Persons(
      Persons other,
      boolean deepCopy,
      BiConsumer<Person, Person> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new PersonsFactory(other.getFactory().getIdGroupingToken(), this);
  }


  /**
   * Retrieve by its xml Id
   * <p>
   * This method is not efficient, since it loops through all the registered entries in order to find the
   * required one.
   *
   * @param xmlId the XML Id of the entity
   * @return the retrieved entity, or null if nothing was found
   */
  public Person getByXmlId(final String xmlId) {
    return firstMatch(person -> xmlId.equals(person.getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PersonsFactory getFactory() {
    return this.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Persons shallowClone() {
    return new Persons(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  public Persons deepClone() {
    return new Persons(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Persons deepCloneWithMapping(
      BiConsumer<Person, Person> mapper) {
    return new Persons(this, true, mapper);
  }
}
