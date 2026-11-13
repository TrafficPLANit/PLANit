package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourParticipantRole;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.logging.Logging;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.goplanit.test.demands.DiscreteDemandsTestUtils.addAccompanyingParticipant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the driver value of a participation in a tour. It says whether the person drives the tour's trips by a
 * mode that needs a driver, such as a car. Without a value the primary participant is assumed to drive and the others
 * to be carried. A value is set on the participation itself, or for the whole tour by naming its driver, and goes along
 * wherever a participation is replaced or copied.
 */
public class TourParticipantDriverTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  private DiscreteDemands discreteDemands;

  private Person registerPerson() {
    return discreteDemands.getPersons().getFactory().registerNew();
  }

  /**
   * Register a tour on the person's schedule with the person as its primary participant
   */
  private Tour registerTourFor(Person primaryParticipant) {
    return discreteDemands.getTours().getFactory().registerNew(primaryParticipant, true);
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(TourParticipantDriverTest.class);
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
   * Without a value, the primary participant is assumed to drive and an accompanying one to be carried
   */
  @Test
  public void testWithoutValueOnlyThePrimaryParticipantIsAssumedToDrive() {
    var jointTour = registerTourFor(registerPerson());
    var accompanying = addAccompanyingParticipant(jointTour, registerPerson(), null);
    var primary = jointTour.getPrimaryParticipation();

    assertNull(primary.getDriver());
    assertTrue(primary.isAssumedAsDriver());
    assertNull(accompanying.getDriver());
    assertFalse(accompanying.isAssumedAsDriver());
  }

  /**
   * A known value says who drives whatever the role: here the accompanying participant drives the primary one
   */
  @Test
  public void testKnownValueOverridesTheRole() {
    var jointTour = registerTourFor(registerPerson());
    var accompanying = addAccompanyingParticipant(jointTour, registerPerson(), true);
    var primary = jointTour.getPrimaryParticipation();
    primary.setDriver(false);

    assertEquals(Boolean.FALSE, primary.getDriver());
    assertFalse(primary.isAssumedAsDriver());
    assertEquals(Boolean.TRUE, accompanying.getDriver());
    assertTrue(accompanying.isAssumedAsDriver());
  }

  /**
   * A tour's driver is known once one of its participations has a value
   */
  @Test
  public void testTourDriverIsKnownOnceOneParticipationHasAValue() {
    var jointTour = registerTourFor(registerPerson());
    var accompanying = addAccompanyingParticipant(jointTour, registerPerson(), null);
    assertFalse(jointTour.isDriverKnown());

    accompanying.setDriver(false);
    assertTrue(jointTour.isDriverKnown());
  }

  /**
   * A value set on a participation is on the very participation the person's schedule holds, so the tour and the
   * schedule show it alike, and the role stays as it was
   */
  @Test
  public void testValueSetOnTheParticipationShowsOnTheTourAndOnThePersonsSchedule() {
    var accompanyingPerson = registerPerson();
    registerTourFor(accompanyingPerson);
    var jointTour = registerTourFor(registerPerson());
    var accompanying = addAccompanyingParticipant(jointTour, accompanyingPerson, null);

    accompanying.setDriver(true);

    assertEquals(Boolean.TRUE, accompanying.getDriver());
    assertEquals(TourParticipantRole.ACCOMPANYING, accompanying.getRole());
    assertSame(accompanying, jointTour.getParticipantTours().get(1));
    assertEquals(2, accompanyingPerson.getSchedule().size());
    assertSame(accompanying, accompanyingPerson.getSchedule().get(1));
  }

  /**
   * A sub tour's participation is the one on its parent tour's schedule, so a value set on it shows there, in the same
   * place between the parent's trips
   */
  @Test
  public void testValueSetOnSubTourParticipationShowsOnTheParentTourSchedule() {
    var person = registerPerson();
    var parentTour = registerTourFor(person);
    discreteDemands.getTrips().getFactory().registerNew(parentTour, DirectionBound.OUTBOUND, true);
    var subTour = discreteDemands.getTours().getFactory().registerNew(parentTour, true);
    discreteDemands.getTrips().getFactory().registerNew(parentTour, DirectionBound.INBOUND, true);

    var participation = subTour.getPrimaryParticipation();
    participation.setDriver(false);

    assertEquals(Boolean.FALSE, participation.getDriver());
    assertEquals(3, parentTour.getSchedule().size());
    assertSame(participation, parentTour.getSchedule().get(1));
  }

  /**
   * Setting the driver of a tour makes the given participant drive and every other participant not, whatever values
   * they had, on the participations the schedules hold
   */
  @Test
  public void testSetDriverOfTourMakesTheGivenParticipantDriveAndTheOthersNot() {
    var jointTour = registerTourFor(registerPerson());
    var accompanyingPerson = registerPerson();
    var accompanying = addAccompanyingParticipant(jointTour, accompanyingPerson, null);
    var other = addAccompanyingParticipant(jointTour, registerPerson(), true);
    var primary = jointTour.getPrimaryParticipation();

    jointTour.setDriver(accompanying);

    assertEquals(Boolean.FALSE, primary.getDriver());
    assertEquals(Boolean.TRUE, accompanying.getDriver());
    assertEquals(Boolean.FALSE, other.getDriver());
    assertSame(accompanying, accompanyingPerson.getSchedule().getFirst());
  }

  /**
   * Setting the driver of a tour to a participation in another tour is refused, and leaves the values as they were
   */
  @Test
  public void testSetDriverOfTourRefusesAParticipationNotOnIt() {
    var jointTour = registerTourFor(registerPerson());
    var accompanying = addAccompanyingParticipant(jointTour, registerPerson(), null);
    var otherTour = registerTourFor(registerPerson());

    assertThrows(PlanItRunTimeException.class, () -> jointTour.setDriver(otherTour.getPrimaryParticipation()));
    assertNull(jointTour.getPrimaryParticipation().getDriver());
    assertNull(accompanying.getDriver());
  }

  /**
   * A copy of a tour has participations of its own, with the same roles and values
   */
  @Test
  public void testValueIsKeptWhenTheTourIsCopied() {
    var jointTour = registerTourFor(registerPerson());
    addAccompanyingParticipant(jointTour, registerPerson(), true);
    jointTour.getPrimaryParticipation().setDriver(false);

    var copy = (Tour) jointTour.deepClone();

    assertEquals(2, copy.getParticipantTours().size());
    assertSame(copy, copy.getPrimaryParticipation().getTour());
    assertEquals(Boolean.FALSE, copy.getPrimaryParticipation().getDriver());
    assertEquals(TourParticipantRole.ACCOMPANYING, copy.getParticipantTours().get(1).getRole());
    assertEquals(Boolean.TRUE, copy.getParticipantTours().get(1).getDriver());
  }

  /**
   * When the primary participant leaves, the next participant takes over as primary and keeps their value, on the
   * tour and on their schedule
   */
  @Test
  public void testValueIsKeptWhenThePrimaryParticipantLeaves() {
    var jointTour = registerTourFor(registerPerson());
    var accompanyingPerson = registerPerson();
    addAccompanyingParticipant(jointTour, accompanyingPerson, false);

    jointTour.removeParticipant(jointTour.getPrimaryParticipation());

    var promoted = jointTour.getPrimaryParticipation();
    assertEquals(accompanyingPerson, promoted.getPerson());
    assertEquals(Boolean.FALSE, promoted.getDriver());
    assertSame(promoted, accompanyingPerson.getSchedule().getFirst());
  }

  /**
   * A deep copy of the demand holding a joint tour gives the copied tour the copied persons, with the same roles and
   * values, on the copied schedules. The original persons' schedules still hold the original participations
   */
  @Test
  public void testDeepCloneOfDemandWithJointTourKeepsRolesAndValues() {
    var primaryPerson = registerPerson();
    var accompanyingPerson = registerPerson();
    var jointTour = registerTourFor(primaryPerson);
    var accompanying = addAccompanyingParticipant(jointTour, accompanyingPerson, true);
    var primary = jointTour.getPrimaryParticipation();
    primary.setDriver(false);

    var copy = discreteDemands.deepClone();

    var copiedTour = copy.getTours().get(jointTour.getId());
    var copiedPrimaryPerson = copy.getPersons().get(primaryPerson.getId());
    var copiedAccompanyingPerson = copy.getPersons().get(accompanyingPerson.getId());
    assertNotSame(jointTour, copiedTour);
    assertNotSame(primaryPerson, copiedPrimaryPerson);
    assertEquals(2, copiedTour.getParticipantTours().size());

    var copiedPrimary = copiedTour.getPrimaryParticipation();
    assertSame(copiedPrimaryPerson, copiedPrimary.getPerson());
    assertEquals(Boolean.FALSE, copiedPrimary.getDriver());
    assertSame(copiedPrimary, copiedPrimaryPerson.getSchedule().getFirst());

    var copiedAccompanying = copiedTour.getParticipantTours().get(1);
    assertSame(copiedAccompanyingPerson, copiedAccompanying.getPerson());
    assertEquals(TourParticipantRole.ACCOMPANYING, copiedAccompanying.getRole());
    assertEquals(Boolean.TRUE, copiedAccompanying.getDriver());
    assertSame(copiedAccompanying, copiedAccompanyingPerson.getSchedule().getFirst());

    assertSame(primary, primaryPerson.getSchedule().getFirst());
    assertSame(accompanying, accompanyingPerson.getSchedule().getFirst());
  }
}
