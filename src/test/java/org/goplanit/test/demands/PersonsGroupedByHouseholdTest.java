package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests the index of entities grouped by a key, {@code ManagedIdEntities#createGroupByIndex}, on the persons of a
 * demand grouped by their household
 *
 * @author markr
 */
class PersonsGroupedByHouseholdTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /**
   * Two persons of one household, one of another, and one without a household: the index holds the two households,
   * each with its persons, and leaves out the person without one
   */
  @Test
  void testPersonsGroupedByHousehold() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    var discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
    var firstHousehold = discreteDemands.getHouseholds().getFactory().registerNew();
    var secondHousehold = discreteDemands.getHouseholds().getFactory().registerNew();
    var persons = discreteDemands.getPersons().getFactory();
    var firstOfFirst = persons.registerNew(firstHousehold);
    var secondOfFirst = persons.registerNew(firstHousehold);
    var onlyOfSecond = persons.registerNew(secondHousehold);
    var withoutHousehold = persons.registerNew();

    var index = discreteDemands.getPersons().createGroupByIndex(Person::getHousehold);

    assertEquals(2, index.size(), "households in the index");
    assertEquals(Set.of(firstOfFirst, secondOfFirst), new HashSet<>(index.get(firstHousehold)),
        "persons of the first household");
    assertEquals(Set.of(onlyOfSecond), new HashSet<>(index.get(secondHousehold)), "persons of the second household");
    assertFalse(index.values().stream().anyMatch(group -> group.contains(withoutHousehold)),
        "person without a household in the index");
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(PersonsGroupedByHouseholdTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }
}
