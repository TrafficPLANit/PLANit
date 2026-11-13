package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.EmploymentStatus;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.Sex;
import org.goplanit.demands.discrete.person.StudyStatus;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the optional attributes of persons and households. Each is not known until set, a copy of a person or
 * household holds the same values as the original, and what is concluded from them rests only on values that are known.
 */
public class PersonHouseholdAttributesTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /**
   * Assert that none of the person's age, sex, employment status, study status and car licence is known
   */
  private static void assertNoAttributeKnown(Person person) {
    assertNull(person.getAge());
    assertNull(person.getSex());
    assertNull(person.getEmploymentStatus());
    assertNull(person.getStudyStatus());
    assertNull(person.getCarLicence());
  }

  /**
   * Assert that neither the household's number of cars nor its yearly income is known
   */
  private static void assertNoAttributeKnown(Household household) {
    assertNull(household.getNumberOfCars());
    assertNull(household.getYearlyIncome());
  }

  private DiscreteDemands discreteDemands;

  /**
   * Register a person aged 45, female, working full-time, not studying, holding a car licence and starting at home
   */
  private Person registerPersonWithEveryAttribute() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    person.setAge(45);
    person.setSex(Sex.FEMALE);
    person.setEmploymentStatus(EmploymentStatus.FULL_TIME);
    person.setStudyStatus(StudyStatus.NOT_STUDYING);
    person.setCarLicence(true);
    person.setInitialPurpose("home");
    return person;
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(PersonHouseholdAttributesTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  @BeforeEach
  public void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  /**
   * A new person and a new household know none of their attributes, and neither does a copy of them
   */
  @Test
  public void testNewPersonAndHouseholdHaveEveryAttributeNotKnown() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    assertNoAttributeKnown(person);
    assertNoAttributeKnown(person.deepClone());

    var household = discreteDemands.getHouseholds().getFactory().registerNew();
    assertNoAttributeKnown(household);
  }

  /**
   * Setting an attribute to not known clears it
   */
  @Test
  public void testSettingNotKnownClearsAnAttribute() {
    var person = registerPersonWithEveryAttribute();
    assertEquals(Integer.valueOf(45), person.getAge());

    person.setAge(null);
    assertNull(person.getAge());
  }

  /**
   * A shallow and a deep copy of a person hold its attributes and its initial purpose
   */
  @Test
  public void testPersonCopiesKeepAttributesAndInitialPurpose() {
    var person = registerPersonWithEveryAttribute();

    for (Person copy : List.of(person.shallowClone(), person.deepClone())) {
      assertEquals(Integer.valueOf(45), copy.getAge());
      assertEquals(Sex.FEMALE, copy.getSex());
      assertEquals(EmploymentStatus.FULL_TIME, copy.getEmploymentStatus());
      assertEquals(StudyStatus.NOT_STUDYING, copy.getStudyStatus());
      assertEquals(Boolean.TRUE, copy.getCarLicence());
      assertEquals("home", copy.getInitialPurpose());
    }
  }

  /**
   * A shallow and a deep copy of a household hold its number of cars and yearly income
   */
  @Test
  public void testHouseholdCopiesKeepAttributes() {
    var household = discreteDemands.getHouseholds().getFactory().registerNew();
    household.setNumberOfCars(2);
    household.setYearlyIncome(96024.0);

    for (Household copy : List.of(household.shallowClone(), household.deepClone())) {
      assertEquals(Integer.valueOf(2), copy.getNumberOfCars());
      assertEquals(Double.valueOf(96024.0), copy.getYearlyIncome());
    }
  }

  /**
   * A person may drive unless known to hold no car licence or known to be younger than the minimum driving age, so a
   * person of whom neither is known may drive
   */
  @Test
  public void testMayDriveUnlessKnownNotTo() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    assertTrue(person.mayDrive(17));

    person.setAge(16);
    assertFalse(person.mayDrive(17));
    person.setAge(17);
    assertTrue(person.mayDrive(17));

    person.setCarLicence(false);
    assertFalse(person.mayDrive(17));
    person.setCarLicence(true);
    assertTrue(person.mayDrive(17));
  }

  /**
   * A household is known to have no car only when its number of cars is known to be 0
   */
  @Test
  public void testKnownToHaveNoCarOnlyWhenItsNumberOfCarsIsZero() {
    var household = discreteDemands.getHouseholds().getFactory().registerNew();
    assertFalse(household.isKnownToHaveNoCar());

    household.setNumberOfCars(0);
    assertTrue(household.isKnownToHaveNoCar());
    household.setNumberOfCars(1);
    assertFalse(household.isKnownToHaveNoCar());
  }
}
