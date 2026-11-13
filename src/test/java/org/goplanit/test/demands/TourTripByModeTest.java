package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.logging.Logging;
import org.goplanit.mode.ModeFeaturesFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.DriverType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for finding a trip of a tour by its mode. The trips of a sub tour are the sub tour's, not those of the tour
 * holding it
 */
public class TourTripByModeTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  private MacroscopicNetwork network;

  private DiscreteDemands discreteDemands;

  /**
   * Register an outbound trip on the tour's schedule by the given mode
   */
  private void registerTripBy(Tour tour, Mode mode) {
    var trip = discreteDemands.getTrips().getFactory().registerNew(tour, DirectionBound.OUTBOUND, true);
    trip.setMode(mode);
  }

  /**
   * Register an outbound trip on the tour's schedule by the mode of the given type
   */
  private void registerTripBy(Tour tour, PredefinedModeType modeType) {
    registerTripBy(tour, network.getModes().get(modeType));
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(TourTripByModeTest.class);
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
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    network.getModes().getFactory().registerNew(PredefinedModeType.CAR_HIGH_OCCUPANCY);
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  /**
   * A work tour by high occupancy car with an at-work sub tour driven alone has a trip by high occupancy car but none
   * by car, its sub tour one by car; a tour without trips has none
   */
  @Test
  public void testOnlyTheTripsOfTheTourItselfAreFound() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    var workTour = discreteDemands.getTours().getFactory().registerNew(person, true);
    registerTripBy(workTour, PredefinedModeType.CAR_HIGH_OCCUPANCY);
    var atWorkSubTour = discreteDemands.getTours().getFactory().registerNew(workTour, true);
    registerTripBy(atWorkSubTour, PredefinedModeType.CAR);

    assertTrue(workTour.hasTripByMode(Mode::isSelfDrivenMotorisedMode));
    assertFalse(workTour.hasTripByMode(mode -> mode.isPredefinedModeType(PredefinedModeType.CAR)));
    assertTrue(atWorkSubTour.hasTripByMode(mode -> mode.isPredefinedModeType(PredefinedModeType.CAR)));

    var tourWithoutTrips = discreteDemands.getTours().getFactory().registerNew(person, true);
    assertFalse(tourWithoutTrips.hasTripByMode(Mode::isSelfDrivenMotorisedMode));
  }

  /**
   * A tour leaves by the mode of its first trip and returns by that of its last: out by high occupancy car, an at-work
   * sub tour by car in between, and back by car, it leaves by high occupancy car and returns by car; a tour without
   * trips has neither
   */
  @Test
  public void testOutboundModeIsTheFirstTripsAndInboundModeTheLasts() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    var workTour = discreteDemands.getTours().getFactory().registerNew(person, true);
    registerTripBy(workTour, PredefinedModeType.CAR_HIGH_OCCUPANCY);
    var atWorkSubTour = discreteDemands.getTours().getFactory().registerNew(workTour, true);
    registerTripBy(atWorkSubTour, PredefinedModeType.CAR);
    registerTripBy(workTour, PredefinedModeType.CAR);

    var car = network.getModes().get(PredefinedModeType.CAR);
    assertEquals(network.getModes().get(PredefinedModeType.CAR_HIGH_OCCUPANCY), workTour.getOutboundMode());
    assertEquals(car, workTour.getInboundMode());
    assertEquals(car, atWorkSubTour.getOutboundMode());
    assertEquals(car, atWorkSubTour.getInboundMode());

    var tourWithoutTrips = discreteDemands.getTours().getFactory().registerNew(person, true);
    assertNull(tourWithoutTrips.getOutboundMode());
    assertNull(tourWithoutTrips.getInboundMode());
  }

  /**
   * A tour by a custom motorised mode that one of the people travelling in it drives has a trip by a mode driven by one
   * of the people in it
   */
  @Test
  public void testTripByCustomModeIsFound() {
    var van = network.getModes().getFactory().registerNewCustomMode("van", 100, 1.5,
        ModeFeaturesFactory.createPhysicalFeatures(
            VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD),
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE, DriverType.TRAVELLER));
    var person = discreteDemands.getPersons().getFactory().registerNew();
    var tour = discreteDemands.getTours().getFactory().registerNew(person, true);
    registerTripBy(tour, van);

    assertTrue(tour.hasTripByMode(Mode::isSelfDrivenMotorisedMode));
  }
}
