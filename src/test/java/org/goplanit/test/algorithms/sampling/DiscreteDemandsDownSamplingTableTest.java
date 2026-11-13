package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingCarAvailability;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingCheckedTable;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingModeGroup;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.EmploymentStatus;
import org.goplanit.demands.discrete.person.Sex;
import org.goplanit.demands.discrete.person.StudyStatus;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimBuilderSettings;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrixUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable.NOT_KNOWN;
import static org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup.*;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.addAccompanyingParticipant;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.createDemandsWithDayStart;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerHousehold;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerPerson;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTour;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTourThereAndBack;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTrip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The table a down-sampling draws from: the sampling units it forms from a discrete demand, their zones and order,
 * and the counts of every group of totals per sampling unit.
 * <p>
 * Zones A, B and C lie on a line, in that order along the Hilbert curve: A and C by their centroids, B by its polygon
 * only. Zone X has neither. Every mode takes one skim: A to B 3 km, A to C 12 km, B to C 0.5 km. The demand's day
 * starts at 03:00. Its first half: households h1 and h2 in A, h3 in B and h4 in C sharing a tour, and p7 without a
 * household making a tour from C. Its second half: h6 and then h5 in C, and p14 without a household or tours.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingTableTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** the day start of an ActivitySim day */
  private static final LocalTime THREE_AM = LocalTime.of(3, 0);

  /**
   * The value of a sampling unit for a category given by its label
   *
   * @param table of the sampling unit
   * @param unit sampling unit to look up
   * @param group of the category
   * @param category label of the category
   * @return the value, 0 when the table has no such category
   */
  private static int valueOf(
      DiscreteDemandsDownSamplingTable table, int unit, DownSamplingBalanceGroup group, String category) {
    int index = table.getCategories(group).indexOf(category);
    return index < 0 ? 0 : table.getValue(unit, group, index);
  }

  /**
   * The value of a sampling unit for a cell of a checked table given by its label
   *
   * @param table of the sampling unit
   * @param unit sampling unit to look up
   * @param checkedTable of the cell
   * @param cell label of the cell
   * @return the value, 0 when the table has no such cell
   */
  private static int valueOf(
      DiscreteDemandsDownSamplingTable table, int unit, DownSamplingCheckedTable checkedTable, String cell) {
    int index = table.getCategories(checkedTable).indexOf(cell);
    return index < 0 ? 0 : table.getValue(unit, checkedTable, index);
  }

  /**
   * The XML ids of a sampling unit's households and then of its persons without a household, looked up in the demand
   * the table was counted from by the ids it holds
   *
   * @param table of the sampling unit
   * @param demands the table was counted from
   * @param unit sampling unit to look up
   * @return the XML ids
   */
  private static List<String> xmlIdsOf(DiscreteDemandsDownSamplingTable table, DiscreteDemands demands, int unit) {
    var xmlIds = new ArrayList<String>();
    for (long id : table.getHouseholdIds(unit)) {
      xmlIds.add(demands.getHouseholds().get(id).getXmlId());
    }
    for (long id : table.getPersonIdsWithoutHousehold(unit)) {
      xmlIds.add(demands.getPersons().get(id).getXmlId());
    }
    return xmlIds;
  }

  /** id token of the test */
  private final IdGroupingToken token = IdGroupingToken.collectGlobalToken();

  /** the modes */
  private Mode car;

  /** the modes */
  private Mode bus;

  /** the modes */
  private Mode walk;

  /** zoning of zones A, B, C and X */
  private Zoning zoning;

  /** a zone of another zoning */
  private OdZone otherZoningZone;

  /** the zones */
  private OdZone a;

  /** the zones */
  private OdZone b;

  /** the zones */
  private OdZone c;

  /** the zone with neither polygon nor centroid position */
  private OdZone x;

  /** the lookup, one skim for every mode */
  private Map<Mode, OdSkimMatrix> distances;

  /** the settings, with two destination areas */
  private DiscreteDemandsDownSamplingSettings settings;

  /** the whole demand of the last {@link #createWholeTable()} */
  private DiscreteDemands wholeDemand;

  /**
   * The sampling unit of the whole demand's table holding the household or the person without a household of the
   * given XML id
   *
   * @param table of the whole demand
   * @param xmlId of the household or person
   * @return the sampling unit
   */
  private int unitOf(DiscreteDemandsDownSamplingTable table, String xmlId) {
    for (int unit = 0; unit < table.size(); ++unit) {
      if (xmlIdsOf(table, wholeDemand, unit).contains(xmlId)) {
        return unit;
      }
    }
    throw new AssertionError("No sampling unit holds " + xmlId);
  }

  /**
   * The first half of the demand: h1 and h2 in A with tours, h3 in B and h4 in C sharing a tour, p7 without a household
   *
   * @param demands to add it to
   */
  private void addFirstHalf(DiscreteDemands demands) {
    /* a tour of two persons by car, the primary one assumed to drive, there and back */
    var h1 = registerHousehold(demands, "h1", a, 1, 10.0);
    var p1 = registerPerson(
        demands, h1, "p1", 40, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.FEMALE);
    var p2 = registerPerson(
        demands, h1, "p2", 38, EmploymentStatus.PART_TIME, StudyStatus.NOT_STUDYING, null, Sex.MALE);
    var shopping = registerTour(demands, p1, a, b, "shopping");
    addAccompanyingParticipant(shopping, p2);
    registerTrip(demands, shopping, DirectionBound.OUTBOUND, car, LocalTime.of(8, 30));
    registerTrip(demands, shopping, DirectionBound.INBOUND, car, LocalTime.of(18, 0));

    /* a commute by bus coming back at 01:30, the next morning, and a walk to school */
    var h2 = registerHousehold(demands, "h2", a, 1, 20.0);
    var p3 = registerPerson(
        demands, h2, "p3", 45, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.FEMALE);
    var p4 = registerPerson(
        demands, h2, "p4", 12, EmploymentStatus.NOT_EMPLOYED, StudyStatus.SCHOOL, null, Sex.MALE);
    var work = registerTour(demands, p3, a, c, "work");
    registerTrip(demands, work, DirectionBound.OUTBOUND, bus, LocalTime.of(7, 0));
    registerTrip(demands, work, DirectionBound.INBOUND, bus, LocalTime.of(1, 30));
    var school = registerTour(demands, p4, a, b, "school");
    registerTrip(demands, school, DirectionBound.OUTBOUND, walk, LocalTime.of(8, 0));
    registerTrip(demands, school, DirectionBound.INBOUND, walk, LocalTime.of(15, 0));

    /* two households sharing a tour, one with nothing known */
    var h3 = registerHousehold(demands, "h3", b, 0, 30.0);
    var p5 = registerPerson(
        demands, h3, "p5", 70, EmploymentStatus.NOT_EMPLOYED, StudyStatus.NOT_STUDYING, false, Sex.OTHER);
    var h4 = registerHousehold(demands, "h4", c, null, null);
    var p6 = registerPerson(demands, h4, "p6", null, null, null, null, null);
    var visit = registerTour(demands, p6, c, a, "visit");
    addAccompanyingParticipant(visit, p5);
    registerTrip(demands, visit, DirectionBound.OUTBOUND, car, LocalTime.of(10, 30));

    /* a person without a household, driving alone */
    var p7 = registerPerson(
        demands, null, "p7", 30, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.MALE);
    var commute = registerTour(demands, p7, c, b, "work");
    registerTrip(demands, commute, DirectionBound.OUTBOUND, car, LocalTime.of(9, 0));
  }

  /**
   * The second half of the demand: h6 and then h5 in C, without tours, and p14 without a household or tours
   *
   * @param demands to add it to
   */
  private void addSecondHalf(DiscreteDemands demands) {
    var h6 = registerHousehold(demands, "h6", c, 2, 50.0);
    registerPerson(demands, h6, "p9", 85, EmploymentStatus.NOT_EMPLOYED, StudyStatus.NOT_STUDYING, null, Sex.FEMALE);
    registerPerson(demands, h6, "p10", 3, EmploymentStatus.NOT_EMPLOYED, StudyStatus.NOT_STUDYING, null, Sex.MALE);
    registerPerson(demands, h6, "p11", 20, EmploymentStatus.PART_TIME, StudyStatus.TERTIARY, true, Sex.FEMALE);
    registerPerson(demands, h6, "p12", 50, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.MALE);
    registerPerson(demands, h6, "p13", 50, null, StudyStatus.NOT_STUDYING, true, Sex.FEMALE);

    var h5 = registerHousehold(demands, "h5", c, 3, 40.0);
    registerPerson(demands, h5, "p8", 30, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.MALE);

    registerPerson(
        demands, null, "p14", 60, EmploymentStatus.NOT_EMPLOYED, StudyStatus.NOT_STUDYING, false, Sex.FEMALE);
  }

  /**
   * The table of the whole demand, kept as {@link #wholeDemand}
   *
   * @return the table
   */
  private DiscreteDemandsDownSamplingTable createWholeTable() {
    wholeDemand = createDemandsWithDayStart(token, THREE_AM);
    addFirstHalf(wholeDemand);
    addSecondHalf(wholeDemand);
    return new DiscreteDemandsDownSamplingTable(settings, zoning, distances, wholeDemand);
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(DiscreteDemandsDownSamplingTableTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  /** Zones, modes, the lookup and the settings, before each test */
  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var network = new MacroscopicNetwork(token);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    walk = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    var layer = network.getTransportLayers().getFactory().registerNew();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    a = zoning.getOdZones().getFactory().registerNew();
    a.setXmlId("A");
    a.getCentroid().setPosition(PlanitJtsUtils.createPoint(0, 0));
    b = zoning.getOdZones().getFactory().registerNew();
    b.setXmlId("B");
    b.setGeometry(PlanitJtsUtils.create2DPolygon(new Envelope(10000, 11000, -500, 500)));
    c = zoning.getOdZones().getFactory().registerNew();
    c.setXmlId("C");
    c.getCentroid().setPosition(PlanitJtsUtils.createPoint(20000, 0));
    x = zoning.getOdZones().getFactory().registerNew();
    x.setXmlId("X");

    var otherZoning = new Zoning(token, layer.getLayerIdGroupingToken());
    otherZoningZone = otherZoning.getOdZones().getFactory().registerNew();
    otherZoningZone.getCentroid().setPosition(PlanitJtsUtils.createPoint(0, 0));

    var skim = OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.DISTANCE);
    for (var pair : new Object[][]{{a, b, 3.0}, {a, c, 12.0}, {b, c, 0.5}}) {
      skim.setValue((OdZone) pair[0], (OdZone) pair[1], (Double) pair[2]);
      skim.setValue((OdZone) pair[1], (OdZone) pair[0], (Double) pair[2]);
    }
    OdSkimMatrixUtils.inferUnreachableCells(skim, car, zoning, new OdSkimBuilderSettings());
    distances = Map.of(car, skim, bus, skim, walk, skim);

    settings = new DiscreteDemandsDownSamplingSettings();
    settings.setNumberOfDestinationAreas(2);
  }

  /** Reset the ids after each test */
  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /** A demand's day starts at the start of its first time period, at midnight when it has none */
  @Test
  public void testDayStartOfTheDemand() {
    assertEquals(THREE_AM, createDemandsWithDayStart(token, THREE_AM).getDayStartTime());
    assertEquals(LocalTime.MIDNIGHT, new DiscreteDemands(token).getDayStartTime());
  }

  /**
   * Seven sampling units: h3 and h4 joined by their tour, in B, the zone of h3, their first household, which is placed
   * by its polygon; p7 one of their own in C, where their tour starts; p14 without a zone, with the zone id in the
   * table after the zones'. Within C the sampling units are in the order given, households first: h6 before h5, as h6 was added first,
   * then p7
   */
  @Test
  public void testUnitsJoinedPlacedAndOrdered() {
    var table = createWholeTable();
    assertEquals(7, table.size());
    assertEquals(4, table.getNumberOfZonesInTable());

    var firstXmlIds = new ArrayList<String>();
    var zoneIdsInTable = new ArrayList<Integer>();
    for (int unit : table.createUnitOrder()) {
      firstXmlIds.add(xmlIdsOf(table, wholeDemand, unit).get(0));
      zoneIdsInTable.add(table.getZoneIdInTable(unit));
    }
    assertEquals(List.of("h1", "h2", "h3", "h6", "h5", "p7", "p14"), firstXmlIds);
    assertEquals(List.of(0, 0, 1, 2, 2, 2, 3), zoneIdsInTable);

    int joined = unitOf(table, "h4");
    assertEquals(List.of("h3", "h4"), xmlIdsOf(table, wholeDemand, joined));
    assertEquals(2, table.getHouseholdIds(joined).length);
    assertEquals(2, table.getPersons(joined));
    assertEquals(1, table.getZoneIdInTable(joined));

    int alone = unitOf(table, "p7");
    assertEquals(0, table.getHouseholdIds(alone).length);
    assertEquals(1, table.getPersonIdsWithoutHousehold(alone).length);
    assertEquals(1, table.getPersons(alone));
    assertEquals(1, table.getValue(alone, PERSONS_PER_ZONE, 0));
  }

  /**
   * h1's tour of two persons by car, 3 km there and back: each trip counts once per person, driven by the primary one
   * and ridden by the other, in the band from 2 to 5 km and in its departure period; the tour counts once
   */
  @Test
  public void testTripsOfAJointTour() {
    var table = createWholeTable();
    int h1 = unitOf(table, "h1");
    var bands = settings.getDistanceBands().getLabels();

    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_MODE, car.getXmlId() + " driven"));
    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_MODE, car.getXmlId() + " ridden"));
    assertEquals(6000, valueOf(table, h1, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.CAR_DRIVER.name()));
    assertEquals(6000,
        valueOf(table, h1, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.CAR_NOT_DRIVER.name()));
    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP,
        DownSamplingModeGroup.CAR_DRIVER + " " + bands.get(2)));
    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP,
        DownSamplingModeGroup.CAR_NOT_DRIVER + " " + bands.get(2)));
    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_DEPARTURE_PERIOD, "08:00:00-10:00:00"));
    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_DEPARTURE_PERIOD, "17:00:00-19:00:00"));
    assertEquals(1, valueOf(table, h1, CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD, "08:00:00-10:00:00"));
    assertEquals(1, valueOf(table, h1, CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD, "17:00:00-19:00:00"));
    assertEquals(1, valueOf(table, h1, TOURS_BY_PURPOSE, "shopping"));
    assertEquals(1, valueOf(table, h1, TOUR_DESTINATIONS_BY_AREA, "2"));

    /* car driven trips of h1, of h3 and h4 joined, and of p7 */
    var modes = table.getCategories(PERSON_TRIPS_BY_MODE);
    assertEquals(4, table.getFullTotal(PERSON_TRIPS_BY_MODE, modes.indexOf(car.getXmlId() + " driven")));
  }

  /**
   * With the skims in metres, h1's joint tour counts as with them in km: 3 km there and back, 6,000 m per mode group
   * and in the band from 2 to 5 km
   */
  @Test
  public void testSkimsInMetres() {
    var inKm = distances.get(car);
    var inMetres =
        OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.DISTANCE, Unit.METER);
    zoning.getOdZones().forEachOriginDestination((origin, destination) -> inMetres.setValue(
        origin, destination, inKm.getValueConvertedTo(origin, destination, Unit.METER)));
    distances = Map.of(car, inMetres, bus, inMetres, walk, inMetres);
    var table = createWholeTable();
    int h1 = unitOf(table, "h1");

    assertEquals(6000, valueOf(table, h1, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.CAR_DRIVER.name()));
    assertEquals(2, valueOf(table, h1, PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP,
        DownSamplingModeGroup.CAR_DRIVER + " " + settings.getDistanceBands().getLabels().get(2)));
  }

  /**
   * h2's commute by bus, 12 km there and back, is public transport, coming back at 01:30 in the period from 19:00 to
   * the day start; the walk to school, 3 km there and back, counts in the active mode group; neither is driven by car
   */
  @Test
  public void testPublicTransportAndTheNight() {
    var table = createWholeTable();
    int h2 = unitOf(table, "h2");
    var bands = settings.getDistanceBands().getLabels();

    assertEquals(2, valueOf(table, h2, PERSON_TRIPS_BY_MODE, bus.getXmlId()));
    assertEquals(2, valueOf(table, h2, PERSON_TRIPS_BY_MODE, walk.getXmlId()));
    assertEquals(24000,
        valueOf(table, h2, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.PUBLIC_TRANSPORT.name()));
    assertEquals(6000, valueOf(table, h2, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.ACTIVE.name()));
    assertEquals(0, valueOf(table, h2, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.CAR_DRIVER.name()));
    assertEquals(2, valueOf(table, h2, PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP,
        DownSamplingModeGroup.PUBLIC_TRANSPORT + " " + bands.get(4)));
    assertEquals(1, valueOf(table, h2, PERSON_TRIPS_BY_DEPARTURE_PERIOD, "06:00:00-08:00:00"));
    assertEquals(1, valueOf(table, h2, PERSON_TRIPS_BY_DEPARTURE_PERIOD, "08:00:00-10:00:00"));
    assertEquals(1, valueOf(table, h2, PERSON_TRIPS_BY_DEPARTURE_PERIOD, "15:00:00-17:00:00"));
    assertEquals(1, valueOf(table, h2, PERSON_TRIPS_BY_DEPARTURE_PERIOD, "19:00:00-03:00:00"));
    for (int period = 0; period < table.getCategories(CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD).size(); ++period) {
      assertEquals(0, table.getValue(h2, CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD, period));
    }
  }

  /** p7, without a household, drives alone 0.5 km from C to B; p14 has no trips and no zone */
  @Test
  public void testPersonsWithoutAHousehold() {
    var table = createWholeTable();
    int p7 = unitOf(table, "p7");
    assertEquals(1, valueOf(table, p7, PERSON_TRIPS_BY_MODE, car.getXmlId() + " driven"));
    assertEquals(500, valueOf(table, p7, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.CAR_DRIVER.name()));
    assertEquals(1, valueOf(table, p7, PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP,
        DownSamplingModeGroup.CAR_DRIVER + " " + settings.getDistanceBands().getLabels().get(0)));
    assertEquals(1, valueOf(table, p7, CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD, "08:00:00-10:00:00"));
    assertEquals(0, valueOf(table, p7, HOUSEHOLDS_BY_SIZE, "1"));

    int p14 = unitOf(table, "p14");
    assertEquals(3, table.getZoneIdInTable(p14));
    assertEquals(0, valueOf(table, p14, PERSON_TRIPS_BY_MODE, car.getXmlId() + " driven"));
    assertEquals(1, valueOf(table, p14, PERSONS_BY_AGE_BAND, "45-64"));
  }

  /**
   * Households and persons: h1's one car for two who may drive is fewer cars than drivers; h2's one car is enough as
   * the child of 12 may not drive; h6's two cars for four who may drive; workers work full or part time only; the
   * values not known of h4 a category of their own
   */
  @Test
  public void testHouseholdsAndPersons() {
    var table = createWholeTable();
    int h1 = unitOf(table, "h1");
    assertEquals(1,
        valueOf(table, h1, HOUSEHOLDS_BY_CAR_AVAILABILITY, DownSamplingCarAvailability.FEWER_CARS_THAN_DRIVERS.name()));
    assertEquals(1, valueOf(table, h1, HOUSEHOLDS_BY_NUMBER_OF_WORKERS, "2"));
    assertEquals(1, valueOf(table, h1, HOUSEHOLDS_BY_NUMBER_OF_CARS, "1"));
    assertEquals(1, valueOf(table, h1, HOUSEHOLDS_BY_SIZE, "2"));
    assertEquals(2, valueOf(table, h1, PERSONS_BY_AGE_BAND, "25-44"));
    assertEquals(1, valueOf(table, h1, PERSONS_BY_EMPLOYMENT_STATUS, EmploymentStatus.FULL_TIME.name()));
    assertEquals(1, valueOf(table, h1, PERSONS_BY_EMPLOYMENT_STATUS, EmploymentStatus.PART_TIME.name()));
    assertEquals(2, valueOf(table, h1, PERSONS_BY_STUDY_STATUS, StudyStatus.NOT_STUDYING.name()));
    assertEquals(1, valueOf(table, h1, PERSONS_BY_CAR_LICENCE, "yes"));
    assertEquals(1, valueOf(table, h1, PERSONS_BY_CAR_LICENCE, NOT_KNOWN));
    assertEquals(1, valueOf(table, h1, PERSONS_BY_SEX, Sex.FEMALE.name()));
    assertEquals(1, valueOf(table, h1, PERSONS_BY_SEX, Sex.MALE.name()));

    int h2 = unitOf(table, "h2");
    assertEquals(1,
        valueOf(table, h2, HOUSEHOLDS_BY_CAR_AVAILABILITY, DownSamplingCarAvailability.A_CAR_PER_DRIVER.name()));
    assertEquals(1, valueOf(table, h2, HOUSEHOLDS_BY_NUMBER_OF_WORKERS, "1"));
    assertEquals(1, valueOf(table, h2, PERSONS_BY_AGE_BAND, "12-17"));

    int h6 = unitOf(table, "h6");
    assertEquals(1,
        valueOf(table, h6, HOUSEHOLDS_BY_CAR_AVAILABILITY, DownSamplingCarAvailability.FEWER_CARS_THAN_DRIVERS.name()));
    assertEquals(1, valueOf(table, h6, HOUSEHOLDS_BY_NUMBER_OF_WORKERS, "2"));
    assertEquals(1, valueOf(table, h6, HOUSEHOLDS_BY_SIZE, ">=5"));
    assertEquals(1, valueOf(table, h6, PERSONS_BY_AGE_BAND, ">=80"));
    assertEquals(1, valueOf(table, h6, PERSONS_BY_AGE_BAND, "0-4"));
    assertEquals(2, valueOf(table, h6, PERSONS_BY_AGE_BAND, "45-64"));

    int joined = unitOf(table, "h3");
    assertEquals(1, valueOf(table, joined, HOUSEHOLDS_BY_CAR_AVAILABILITY, DownSamplingCarAvailability.NO_CAR.name()));
    assertEquals(1, valueOf(table, joined, HOUSEHOLDS_BY_CAR_AVAILABILITY, NOT_KNOWN));
    assertEquals(1, valueOf(table, joined, HOUSEHOLDS_BY_NUMBER_OF_CARS, "0"));
    assertEquals(1, valueOf(table, joined, HOUSEHOLDS_BY_NUMBER_OF_CARS, NOT_KNOWN));
    assertEquals(2, valueOf(table, joined, HOUSEHOLDS_BY_NUMBER_OF_WORKERS, "0"));
    assertEquals(1, valueOf(table, joined, PERSONS_BY_AGE_BAND, "65-79"));
    assertEquals(1, valueOf(table, joined, PERSONS_BY_AGE_BAND, NOT_KNOWN));
    assertEquals(1, valueOf(table, joined, PERSONS_BY_CAR_LICENCE, "no"));
    assertEquals(1, valueOf(table, joined, PERSONS_BY_SEX, Sex.OTHER.name()));
    assertEquals(1, valueOf(table, joined, PERSONS_BY_SEX, NOT_KNOWN));
  }

  /**
   * Income bands are the fifths of the known incomes of the whole demand, 10 to 50, one household each, h4's not
   * known; the tour destinations, one in A, three in B and one in C, binned along the curve into two areas at the third
   * destination, in B: A, and B with C
   */
  @Test
  public void testIncomeFifthsAndDestinationAreas() {
    var table = createWholeTable();
    var incomeBands = table.getCategories(HOUSEHOLDS_BY_INCOME_BAND);
    assertEquals(6, incomeBands.size());
    assertEquals(NOT_KNOWN, incomeBands.get(5));
    for (int band = 0; band < incomeBands.size(); ++band) {
      assertEquals(1, table.getFullTotal(HOUSEHOLDS_BY_INCOME_BAND, band));
    }
    int joined = unitOf(table, "h3");
    assertEquals(1, table.getValue(joined, HOUSEHOLDS_BY_INCOME_BAND, 2));
    assertEquals(1, table.getValue(joined, HOUSEHOLDS_BY_INCOME_BAND, 5));
    assertEquals(1, table.getValue(unitOf(table, "h1"), HOUSEHOLDS_BY_INCOME_BAND, 0));

    assertEquals(List.of("1", "2"), table.getCategories(TOUR_DESTINATIONS_BY_AREA));
    assertEquals(1, table.getFullTotal(TOUR_DESTINATIONS_BY_AREA, 0));
    assertEquals(4, table.getFullTotal(TOUR_DESTINATIONS_BY_AREA, 1));
    int h2 = unitOf(table, "h2");
    assertEquals(0, table.getValue(h2, TOUR_DESTINATIONS_BY_AREA, 0));
    assertEquals(2, table.getValue(h2, TOUR_DESTINATIONS_BY_AREA, 1));
    assertEquals(1, table.getValue(joined, TOUR_DESTINATIONS_BY_AREA, 0));
  }

  /**
   * The bands of the settings rather than the defaults: age bands 0-17, 18-64 and 65 and over put h6's child of 3 in
   * the first, its persons of 20 and twice 50 in the second and its person of 85 in the last; household sizes 0, 1-2
   * and 3 and over; cars 0 and 1 or more; workers 0-1 and 2 or more; and the halves of the known incomes rather than
   * the fifths, with h4's not known
   */
  @Test
  public void testBandsOfTheSettings() {
    settings.setAgeBandBoundaries(18, 65);
    settings.setHouseholdSizeBandBoundaries(1, 3);
    settings.setCarsPerHouseholdBandBoundaries(1);
    settings.setWorkersPerHouseholdBandBoundaries(2);
    settings.setNumberOfIncomeBands(2);
    var table = createWholeTable();

    int h6 = unitOf(table, "h6");
    assertEquals(1, valueOf(table, h6, PERSONS_BY_AGE_BAND, "0-17"));
    assertEquals(3, valueOf(table, h6, PERSONS_BY_AGE_BAND, "18-64"));
    assertEquals(1, valueOf(table, h6, PERSONS_BY_AGE_BAND, ">=65"));
    assertEquals(1, valueOf(table, h6, HOUSEHOLDS_BY_SIZE, ">=3"));
    assertEquals(1, valueOf(table, h6, HOUSEHOLDS_BY_NUMBER_OF_CARS, ">=1"));
    assertEquals(1, valueOf(table, h6, HOUSEHOLDS_BY_NUMBER_OF_WORKERS, ">=2"));
    int h2 = unitOf(table, "h2");
    assertEquals(1, valueOf(table, h2, HOUSEHOLDS_BY_SIZE, "1-2"));
    assertEquals(1, valueOf(table, h2, HOUSEHOLDS_BY_NUMBER_OF_WORKERS, "0-1"));
    assertEquals(1, valueOf(table, unitOf(table, "h3"), HOUSEHOLDS_BY_NUMBER_OF_CARS, "0"));

    var incomeBands = table.getCategories(HOUSEHOLDS_BY_INCOME_BAND);
    assertEquals(3, incomeBands.size());
    assertEquals(NOT_KNOWN, incomeBands.get(2));
  }

  /**
   * h1's checked cells: its car trips out at 08:30 and back at 18:00, driven by p1 and ridden by p2, by mode and hour
   * and by mode and distance band, 3 km; between the areas, A in area 1 and B in area 2, by car as driver for the
   * driver and not as driver for the rider; its shopping tour by car; its persons by age band and sex; and its car
   * driven trips from A and from B
   */
  @Test
  public void testCheckedCells() {
    var table = createWholeTable();
    int h1 = unitOf(table, "h1");
    String driven = car.getXmlId() + " driven";
    String ridden = car.getXmlId() + " ridden";
    var band = settings.getDistanceBands().getLabels().get(2);

    assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_HOUR, driven + " 08"));
    assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_HOUR, ridden + " 08"));
    assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_HOUR, driven + " 18"));
    assertEquals(2,
        valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_DISTANCE_BAND, driven + " " + band));
    assertEquals(2,
        valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_DISTANCE_BAND, ridden + " " + band));
    for (var areas : List.of("1 to 2", "2 to 1")) {
      assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_AREAS_AND_MODE_GROUP,
          DownSamplingModeGroup.CAR_DRIVER + " " + areas));
      assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSON_TRIPS_BY_AREAS_AND_MODE_GROUP,
          DownSamplingModeGroup.CAR_NOT_DRIVER + " " + areas));
    }
    assertEquals(1,
        valueOf(table, h1, DownSamplingCheckedTable.TOURS_BY_PURPOSE_AND_MODE, "shopping " + car.getXmlId()));
    assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSONS_BY_AGE_BAND_AND_SEX, "25-44 FEMALE"));
    assertEquals(1, valueOf(table, h1, DownSamplingCheckedTable.PERSONS_BY_AGE_BAND_AND_SEX, "25-44 MALE"));

    assertEquals(1, table.getCarDrivenTrips(h1, a));
    assertEquals(1, table.getCarDrivenTrips(h1, b));
    assertEquals(0, table.getCarDrivenTrips(h1, c));
  }

  /** A tour counts by its first trip's mode: a work tour out by bus and back on foot is a work tour by bus */
  @Test
  public void testTourByItsFirstTripsMode() {
    var demands = createDemandsWithDayStart(token, THREE_AM);
    var household = registerHousehold(demands, "h30", a, 0, 10.0);
    var person = registerPerson(
        demands, household, "p30", 40, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, false, Sex.MALE);
    var work = registerTour(demands, person, a, b, "work");
    registerTrip(demands, work, DirectionBound.OUTBOUND, bus, LocalTime.of(7, 0));
    registerTrip(demands, work, DirectionBound.INBOUND, walk, LocalTime.of(17, 0));
    var table = new DiscreteDemandsDownSamplingTable(settings, zoning, distances, demands);

    assertEquals(List.of("work " + bus.getXmlId()),
        table.getCategories(DownSamplingCheckedTable.TOURS_BY_PURPOSE_AND_MODE));
    assertEquals(1, table.getValue(0, DownSamplingCheckedTable.TOURS_BY_PURPOSE_AND_MODE, 0));
  }

  /**
   * A tour by car from A to X, a zone without polygon or centroid position, is counted rather than refused, beside a
   * commute by car from A to B, 3 km: the visit's destination is not known while the commute's is in area 1, which
   * starts at B and so holds A, before it; the visit's trip back from X is not a car driven trip of any origin zone,
   * so A has 2 and B 1; the visit's trips have no distance, as the skim cannot infer one for X, so the car driven
   * metres are the commute's 6,000; and between areas they go from area 1 to the area not known and back
   */
  @Test
  public void testTourToAZoneWithoutAPlace() {
    var demands = createDemandsWithDayStart(token, THREE_AM);
    var household = registerHousehold(demands, "h40", a, 1, 10.0);
    var person = registerPerson(
        demands, household, "p40", 40, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.MALE);
    registerTourThereAndBack(demands, List.of(person), a, b, "work", car, LocalTime.of(7, 30), LocalTime.of(17, 30));
    registerTourThereAndBack(demands, List.of(person), a, x, "visit", car, LocalTime.of(10, 0), LocalTime.of(12, 0));
    var table = new DiscreteDemandsDownSamplingTable(settings, zoning, distances, demands);
    String driven = car.getXmlId() + " driven";

    assertEquals(1, table.size());
    assertEquals(1, valueOf(table, 0, TOUR_DESTINATIONS_BY_AREA, NOT_KNOWN));
    assertEquals(1, valueOf(table, 0, TOUR_DESTINATIONS_BY_AREA, "1"));
    assertEquals(2, table.getCarDrivenTrips(0, a));
    assertEquals(1, table.getCarDrivenTrips(0, b));
    assertEquals(0, table.getCarDrivenTrips(0, x));
    assertEquals(2,
        valueOf(table, 0, DownSamplingCheckedTable.PERSON_TRIPS_BY_MODE_AND_DISTANCE_BAND, driven + " " + NOT_KNOWN));
    assertEquals(6000,
        valueOf(table, 0, PERSON_KILOMETRES_BY_MODE_GROUP, DownSamplingModeGroup.CAR_DRIVER.toString()));
    for (var areas : List.of("1 to " + NOT_KNOWN, NOT_KNOWN + " to 1")) {
      assertEquals(1, valueOf(table, 0, DownSamplingCheckedTable.PERSON_TRIPS_BY_AREAS_AND_MODE_GROUP,
          DownSamplingModeGroup.CAR_DRIVER + " " + areas));
    }
  }

  /** Refused: a household in a zone of another zoning, one in a zone without a place, and a tour to a zone of another
   * zoning */
  @Test
  public void testRefusals() {
    var otherZoning = createDemandsWithDayStart(token, THREE_AM);
    registerHousehold(otherZoning, "h20", otherZoningZone, 1, 10.0);
    assertThrows(IllegalArgumentException.class,
        () -> new DiscreteDemandsDownSamplingTable(settings, zoning, distances, otherZoning));

    var noPlace = createDemandsWithDayStart(token, THREE_AM);
    registerHousehold(noPlace, "h21", x, 1, 10.0);
    assertThrows(IllegalArgumentException.class,
        () -> new DiscreteDemandsDownSamplingTable(settings, zoning, distances, noPlace));

    var tourOfOtherZoning = createDemandsWithDayStart(token, THREE_AM);
    var traveller = registerPerson(tourOfOtherZoning, registerHousehold(tourOfOtherZoning, "h23", a, 1, 10.0), "p23",
        30, EmploymentStatus.FULL_TIME, StudyStatus.NOT_STUDYING, true, Sex.MALE);
    registerTour(tourOfOtherZoning, traveller, a, otherZoningZone, "work");
    assertThrows(IllegalArgumentException.class,
        () -> new DiscreteDemandsDownSamplingTable(settings, zoning, distances, tourOfOtherZoning));
  }
}
