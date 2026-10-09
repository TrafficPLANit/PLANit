package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.household.Household;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory class for person instances to be registered on its parent container passed in to constructor
 */
public class PersonsFactory extends ManagedIdEntityFactoryImpl<Person>
    implements ManagedIdEntityFactory<Person> {

  /** container to use */
  protected final Persons persons;

  /**
   * Create a newly created instance without registering on the container
   *
   * @return created time period
   */
  protected Person createNew() {
    return new Person(getIdGroupingToken());
  }

  /**
   * Constructor
   *
   * @param tokenId    to use
   * @param persons to use
   */
  protected PersonsFactory(final IdGroupingToken tokenId, final Persons persons) {
    super(tokenId);
    this.persons = persons;
  }

  /**
   * register a new entry on the container and return it
   *
   * @return created instance
   */
  public Person registerNew() {
    var newInstance = new Person(getIdGroupingToken());
    persons.register(newInstance);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param household to link person to
   * @return created instance
   */
  public Person registerNew(Household household) {
    var newInstance = registerNew();
    newInstance.setHousehold(household);
    return newInstance;
  }

}
