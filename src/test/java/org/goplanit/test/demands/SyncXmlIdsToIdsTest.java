package org.goplanit.test.demands;

import org.goplanit.demands.Demands;
import org.goplanit.demands.DemandsModifierUtils;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.DiscreteDemandsModifierUtils;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests syncing the XML ids of demand entities to their internal ids, as the PLANit demand readers do when asked to:
 * the ids are recreated, and every entity's XML id becomes its new internal id
 *
 * @author markr
 */
class SyncXmlIdsToIdsTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
  }

  /**
   * Discrete demands: a household, two persons, and a tour with two trips, each with an XML id of its own. The first
   * person is removed, so recreating the ids gives the second person id 0; afterwards every entity's XML id is its id
   */
  @Test
  void testDiscreteDemandsXmlIdsSyncedToIds() {
    var discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
    var household = discreteDemands.getHouseholds().getFactory().registerNew();
    household.setXmlId("household");
    var removed = discreteDemands.getPersons().getFactory().registerNew(household);
    removed.setXmlId("removed person");
    var person = discreteDemands.getPersons().getFactory().registerNew(household);
    person.setXmlId("person");
    var tour = discreteDemands.getTours().getFactory().registerNew(person, true);
    tour.setXmlId("tour");
    var outbound = discreteDemands.getTrips().getFactory().registerNew(tour, DirectionBound.OUTBOUND, true);
    outbound.setXmlId("outbound");
    var inbound = discreteDemands.getTrips().getFactory().registerNew(tour, DirectionBound.INBOUND, true);
    inbound.setXmlId("inbound");
    discreteDemands.getDiscreteDemandsModifier().removePerson(removed);

    DiscreteDemandsModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(discreteDemands);

    assertEquals(0, person.getId(), "person's id not recreated");
    for (ExternalIdAble entity : List.<ExternalIdAble>of(household, person, tour, outbound, inbound)) {
      assertEquals(String.valueOf(entity.getId()), entity.getXmlId(), "XML id not synced to the id");
    }
  }

  /** Demands: a time period and two traveller types, each with an XML id of its own, afterwards each XML id is its id */
  @Test
  void testDemandsXmlIdsSyncedToIds() {
    var demands = new Demands(IdGroupingToken.collectGlobalToken());
    var timePeriod = demands.timePeriods.getFactory().registerNew("morning", 0, 3600);
    timePeriod.setXmlId("morning");
    var firstType = demands.travelerTypes.getFactory().registerNew("first");
    firstType.setXmlId("first type");
    var secondType = demands.travelerTypes.getFactory().registerNew("second");
    secondType.setXmlId("second type");

    DemandsModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(demands);

    for (ExternalIdAble entity : List.<ExternalIdAble>of(timePeriod, firstType, secondType)) {
      assertEquals(String.valueOf(entity.getId()), entity.getXmlId(), "XML id not synced to the id");
    }
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(SyncXmlIdsToIdsTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  /**
   * Two discrete demands made one after the other with the same token, as when an input is read twice in one process:
   * each numbers its own households, persons, tours and trips from 0, so the second read gives each the id the first
   * did, and draws seeded by those ids come out the same
   */
  @Test
  public void testDiscreteDemandsNumberTheirOwnEntities() {
    for (int read = 0; read < 2; ++read) {
      var discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
      var household = discreteDemands.getHouseholds().getFactory().registerNew();
      var person = discreteDemands.getPersons().getFactory().registerNew(household);
      var tour = discreteDemands.getTours().getFactory().registerNew(person, true);
      var trip = discreteDemands.getTrips().getFactory().registerNew(tour, DirectionBound.OUTBOUND, true);
      for (ExternalIdAble entity : List.<ExternalIdAble>of(household, person, tour, trip)) {
        assertEquals(0, entity.getId(), "entity not numbered from 0 within its demand");
      }
    }
  }
}
