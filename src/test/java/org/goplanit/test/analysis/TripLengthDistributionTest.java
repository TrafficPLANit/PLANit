package org.goplanit.test.analysis;

import org.goplanit.analysis.distribution.TripLengthDistribution;
import org.goplanit.analysis.distribution.TripLengthDistributionCsvWriter;
import org.goplanit.analysis.distribution.TripLengthDistributionFactory;
import org.goplanit.demands.Demands;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripUtils;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.userclass.UserClass;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.binning.BinBuilder;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdDemandMatrix;
import org.goplanit.zoning.zonetozone.OdSkimBuilderSettings;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrixUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.goplanit.test.demands.DiscreteDemandsTestUtils.addAccompanyingParticipant;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Trip length distributions: trips and the distance they add up to per distance bin, in km unless another distance
 * unit is given, per mode from a discrete demand and per user class from OD demand, and their CSV. Zones A and B lie
 * 1 km apart in a plane in metres, 3 km apart by the car skim.
 *
 * @author markr
 */
public class TripLengthDistributionTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** tolerance on trips and kilometres */
  private static final double TOLERANCE = 1e-5;

  /**
   * Bins of 0 to 1, 1 to 5 and 5 to 40 km, each holding its lower bound, the last also its upper bound
   *
   * @return the bins
   */
  private static BinningConfiguration<Double> bins() {
    return BinBuilder.<Double>create()
        .addBin(0.0, 1.0, true, false, null)
        .addBin(1.0, 5.0, true, false, null)
        .addBin(5.0, 40.0, true, true, null)
        .build();
  }

  /** id token of the test */
  private final IdGroupingToken token = IdGroupingToken.collectGlobalToken();

  /** car mode */
  private Mode car;

  /** zoning of zones A and B */
  private Zoning zoning;

  /** zone A */
  private OdZone a;

  /** zone B */
  private OdZone b;

  /** the inferred distance skims, with car's of 3 km from A to B */
  private Map<Mode, OdSkimMatrix> distances;

  /** OD demand of 2.5 car trips from A to B in the morning */
  private Demands odDemands;

  /** user class of the OD demand */
  private UserClass carUsers;

  /** time period of the OD demand */
  private TimePeriod morning;

  /**
   * Register a person's tour from A to B, from 08:00 to 17:00, with its outbound trip by car
   *
   * @param discreteDemands to register in
   * @return the outbound trip
   */
  private Trip registerOutboundCarTrip(DiscreteDemands discreteDemands) {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    var tour = discreteDemands.getTours().getFactory().registerNew(
        person, a, b, LocalTime.of(8, 0), LocalTime.of(17, 0), true);
    Trip outbound = discreteDemands.getTrips().getFactory().registerNew(tour, DirectionBound.OUTBOUND, true);
    outbound.setMode(car);
    return outbound;
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(TripLengthDistributionTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  /** Zones A and B, the skims and the OD demand, before each test */
  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var network = new MacroscopicNetwork(token);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var layer = network.getTransportLayers().getFactory().registerNew();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    a = zoning.getOdZones().getFactory().registerNew();
    a.setXmlId("A");
    a.getCentroid().setPosition(PlanitJtsUtils.createPoint(0, 0));
    b = zoning.getOdZones().getFactory().registerNew();
    b.setXmlId("B");
    b.getCentroid().setPosition(PlanitJtsUtils.createPoint(1000, 0));

    var skim = new OdSkimMatrix(zoning.getOdZones(), SkimSubOutputType.DISTANCE);
    skim.setValue(a, a, Double.NaN);
    skim.setValue(a, b, 3.0);
    skim.setValue(b, a, 3.0);
    skim.setValue(b, b, Double.NaN);
    OdSkimMatrixUtils.inferUnreachableCells(skim, car, zoning, new OdSkimBuilderSettings());
    distances = Map.of(car, skim);

    odDemands = new Demands(token);
    var travellerType = odDemands.travelerTypes.getFactory().registerNew("default");
    carUsers = odDemands.userClasses.getFactory().registerNew("car users", car, travellerType);
    morning = odDemands.timePeriods.getFactory().registerNew("morning", 7 * 3600, 3600);
    var matrix = new OdDemandMatrix(zoning.getOdZones());
    matrix.setValue(a, b, 2.5);
    odDemands.registerOdDemandPcuHour(morning, car, matrix);
  }

  /** Reset the ids after each test */
  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /** Trips of 0.5, 1.5, 7 and 50 km in bins of 0 to 1, 1 to 5 and 5 to 40 km: one per bin, the 50 km out of range */
  @Test
  public void testBinsOfDifferentWidths() {
    var distribution = new TripLengthDistribution<Mode>(bins());
    for (double km : new double[]{0.5, 1.5, 7, 50}) {
      distribution.add(car, km, 1);
    }
    assertEquals(Set.of(car), distribution.getKeys());
    double[] binKm = {0.5, 1.5, 7};
    for (int bin = 0; bin < 3; ++bin) {
      assertEquals(1.0, distribution.getTrips(car, bin), TOLERANCE);
      assertEquals(binKm[bin], distribution.getDistance(car, bin), TOLERANCE);
      assertEquals(binKm[bin], distribution.getMeanLength(car, bin), TOLERANCE);
      assertEquals(0.25, distribution.getShareOfTrips(car, bin), TOLERANCE);
    }
    assertEquals(1.0, distribution.getOutOfRangeTrips(car), TOLERANCE);
    assertEquals(50.0, distribution.getOutOfRangeDistance(car), TOLERANCE);
    assertEquals(4.0, distribution.getTotalTrips(car), TOLERANCE);
  }

  /** Two persons share a tour whose outbound car trip is 3 km: two car trips and 6 km in the bin of 1 to 5 km */
  @Test
  public void testJointTourCountsEachParticipant() {
    var discreteDemands = new DiscreteDemands(token);
    var outbound = registerOutboundCarTrip(discreteDemands);
    addAccompanyingParticipant(outbound.getTour(), discreteDemands.getPersons().getFactory().registerNew());

    var distribution = TripLengthDistributionFactory.create(discreteDemands, distances, bins());
    assertEquals(2.0, distribution.getTrips(car, 1), TOLERANCE);
    assertEquals(6.0, distribution.getDistance(car, 1), TOLERANCE);
    assertEquals(2.0, distribution.getTotalTrips(car), TOLERANCE);
  }

  /** A user class's matrix holds 2.5 trips from A to B, 3 km apart: 2.5 trips and 7.5 km in the bin of 1 to 5 km */
  @Test
  public void testFractionalOdDemand() {
    var distribution = TripLengthDistributionFactory.create(odDemands, morning, distances, bins());
    assertEquals(Set.of(carUsers), distribution.getKeys());
    assertEquals(2.5, distribution.getTrips(carUsers, 1), TOLERANCE);
    assertEquals(7.5, distribution.getDistance(carUsers, 1), TOLERANCE);
    assertEquals(2.5, distribution.getTotalTrips(carUsers), TOLERANCE);
  }

  /**
   * A distribution in metres, its bins of 0 to 1,000, 1,000 to 5,000 and 5,000 to 40,000 m, from the car skim in km: a
   * car trip from A to B is 3,000 m in the bin of 1,000 to 5,000 m, as are a user class's 2.5 trips, 7,500 m; the
   * CSV's bounds and distances are named in metres
   */
  @Test
  public void testDistributionInMetres(@TempDir Path folder) throws IOException {
    var binsInMetres = BinBuilder.<Double>create()
        .addBin(0.0, 1_000.0, true, false, null)
        .addBin(1_000.0, 5_000.0, true, false, null)
        .addBin(5_000.0, 40_000.0, true, true, null)
        .build();
    var discreteDemands = new DiscreteDemands(token);
    registerOutboundCarTrip(discreteDemands);

    var byMode = TripLengthDistributionFactory.create(discreteDemands, distances, binsInMetres, Unit.METER);
    assertEquals(Unit.METER, byMode.getDistanceUnit());
    assertEquals(1.0, byMode.getTrips(car, 1), TOLERANCE);
    assertEquals(3_000.0, byMode.getDistance(car, 1), TOLERANCE);
    assertEquals(3_000.0, byMode.getMeanLength(car, 1), TOLERANCE);

    var byUserClass = TripLengthDistributionFactory.create(odDemands, morning, distances, binsInMetres, Unit.METER);
    assertEquals(7_500.0, byUserClass.getDistance(carUsers, 1), TOLERANCE);

    var file = folder.resolve("tld.csv");
    TripLengthDistributionCsvWriter.writeByMode(byMode, file);
    assertEquals(List.of("mode", "user_class", "traveller_type", "bin", "lower_m", "upper_m", "trips", "m"),
        List.of(Files.readAllLines(file).get(0).split(",", -1)));
  }

  /**
   * A car skim in metres, 3,000 from A to B: an outbound car trip from A to B is 3 km long all the same, or 3,000 read
   * as the skim holds it, and a user class's 2.5 trips from A to B are 7.5 km in the bin of 1 to 5 km
   */
  @Test
  public void testSkimInMetres() {
    var skim = new OdSkimMatrix(zoning.getOdZones(), SkimSubOutputType.DISTANCE, Unit.METER);
    skim.setValue(a, b, 3_000.0);
    var metres = Map.of(car, skim);

    var outbound = registerOutboundCarTrip(new DiscreteDemands(token));
    assertEquals(3.0, TripUtils.getDistance(outbound, metres, Unit.KM), TOLERANCE);
    assertEquals(3_000.0, TripUtils.getDistance(outbound, metres), TOLERANCE);

    var distribution = TripLengthDistributionFactory.create(odDemands, morning, metres, bins());
    assertEquals(7.5, distribution.getDistance(carUsers, 1), TOLERANCE);
  }

  /** A car distribution in three bins written: a header, three rows for car, and one for the trips out of range */
  @Test
  public void testWrittenToCsv(@TempDir Path folder) throws IOException {
    var distribution = new TripLengthDistribution<Mode>(bins());
    distribution.add(car, 0.5, 2);
    distribution.add(car, 50, 1);
    var file = folder.resolve("tld.csv");
    TripLengthDistributionCsvWriter.writeByMode(distribution, file);

    List<String> lines = Files.readAllLines(file);
    assertEquals(5, lines.size());
    assertEquals(TripLengthDistributionCsvWriter.createHeaders(Unit.KM).size(), lines.get(0).split(",", -1).length);
    for (var line : lines.subList(1, 5)) {
      assertEquals(car.getXmlId(), line.split(",", -1)[0]);
    }
    /* the trips are the second column from the end, as a bin's label holds a comma and is quoted */
    var firstBin = lines.get(1).split(",", -1);
    var outOfRange = lines.get(4).split(",", -1);
    assertEquals("2.0", firstBin[firstBin.length - 2]);
    assertEquals("1.0", outOfRange[outOfRange.length - 2]);
    assertEquals("50.0", outOfRange[outOfRange.length - 1]);
  }
}
