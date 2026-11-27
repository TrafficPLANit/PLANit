package org.goplanit.test.zoning;

import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.zoning.zonetozone.IntrazonalDistanceMethod;
import org.goplanit.zoning.zonetozone.OdSkimBuilderSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings of an OD skim builder: their defaults, the values refused, the proxy modes, and a copy apart from the
 * original
 *
 * @author markr
 */
public class OdSkimBuilderSettingsTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** car mode */
  private Mode car;

  /** taxi mode */
  private Mode taxi;

  /** bus mode */
  private Mode bus;

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(OdSkimBuilderSettingsTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  /** Car, taxi and bus, before each test */
  @BeforeEach
  public void setUp() {
    var modes = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken()).getModes();
    car = modes.getFactory().registerNew(PredefinedModeType.CAR);
    taxi = modes.getFactory().registerNew(PredefinedModeType.TAXI);
    bus = modes.getFactory().registerNew(PredefinedModeType.BUS);
  }

  /**
   * Every setting starts at its default, the threads at one less than the machine's processors and at least 1; the
   * cells the search cannot reach are left without a value, and no mode has a straight line speed
   */
  @Test
  public void testDefaults() {
    var settings = new OdSkimBuilderSettings();
    assertEquals(25.0, settings.getConnectoidSpeedKmh());
    assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() - 1), settings.getNumberOfThreads());
    assertFalse(settings.isInferUnreachableCells());
    assertEquals(1.3, settings.getDetourFactor());
    assertEquals(IntrazonalDistanceMethod.NEAREST_NEIGHBOURS, settings.getIntrazonalDistanceMethod());
    assertEquals(3, settings.getNumberOfNearestZones());
    assertEquals(0.5, settings.getNearestZonesFactor());
    assertTrue(Double.isNaN(settings.getStraightLineSpeedKmhFor(car)));
  }

  /** Each value that cannot be used is refused, and the setting keeps the value it had */
  @Test
  public void testRefusals() {
    var settings = new OdSkimBuilderSettings();
    int threadsBefore = settings.getNumberOfThreads();

    assertThrows(IllegalArgumentException.class, () -> settings.setConnectoidSpeedKmh(0));
    assertEquals(25.0, settings.getConnectoidSpeedKmh());
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfThreads(0));
    assertEquals(threadsBefore, settings.getNumberOfThreads());
    int processors = Runtime.getRuntime().availableProcessors();
    if (processors > 1) {
      assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfThreads(processors));
      assertEquals(threadsBefore, settings.getNumberOfThreads());
    }
    assertThrows(IllegalArgumentException.class, () -> settings.setDetourFactor(0.9));
    assertEquals(1.3, settings.getDetourFactor());
    assertThrows(IllegalArgumentException.class, () -> settings.setIntrazonalDistanceMethod(null));
    assertEquals(IntrazonalDistanceMethod.NEAREST_NEIGHBOURS, settings.getIntrazonalDistanceMethod());
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfNearestZones(0));
    assertEquals(3, settings.getNumberOfNearestZones());
    assertThrows(IllegalArgumentException.class, () -> settings.setNearestZonesFactor(0));
    assertEquals(0.5, settings.getNearestZonesFactor());
    settings.setStraightLineSpeedKmhFor(car, 30);
    assertThrows(IllegalArgumentException.class, () -> settings.setStraightLineSpeedKmhFor(car, 0));
    assertThrows(IllegalArgumentException.class, () -> settings.setStraightLineSpeedKmhFor(null, 30));
    assertEquals(30.0, settings.getStraightLineSpeedKmhFor(car));

    /* the bounds themselves are allowed */
    settings.setNumberOfThreads(1);
    assertEquals(1, settings.getNumberOfThreads());
    settings.setDetourFactor(1);
    assertEquals(1.0, settings.getDetourFactor());
  }

  /**
   * No mode names a proxy mode unless set, so each gives itself. A proxy mode is always one step away: a mode whose
   * skims another takes cannot take another's itself, and a mode taking another's cannot lend its own. Naming a mode
   * its own proxy mode removes its entry
   */
  @Test
  public void testProxyModes() {
    var settings = new OdSkimBuilderSettings();
    assertEquals(taxi, settings.getProxyModeFor(taxi));

    settings.setProxyModeFor(taxi, car);
    assertEquals(car, settings.getProxyModeFor(taxi));
    assertThrows(IllegalArgumentException.class, () -> settings.setProxyModeFor(car, bus));
    assertThrows(IllegalArgumentException.class, () -> settings.setProxyModeFor(bus, taxi));
    assertThrows(IllegalArgumentException.class, () -> settings.setProxyModeFor(bus, null));
    assertEquals(car, settings.getProxyModeFor(taxi));
    assertEquals(car, settings.getProxyModeFor(car));
    assertEquals(bus, settings.getProxyModeFor(bus));

    settings.setProxyModeFor(taxi, taxi);
    assertEquals(taxi, settings.getProxyModeFor(taxi));
    /* car lends its skims to no mode now, so it may take another's */
    settings.setProxyModeFor(car, bus);
    assertEquals(bus, settings.getProxyModeFor(car));
  }

  /** A copy can be changed without changing the original */
  @Test
  public void testShallowCloneApart() {
    var settings = new OdSkimBuilderSettings();
    var copy = settings.shallowClone();
    copy.setConnectoidSpeedKmh(5);
    copy.setNumberOfThreads(1);
    copy.setProxyModeFor(taxi, car);
    copy.setInferUnreachableCells(true);
    copy.setDetourFactor(1.5);
    copy.setIntrazonalDistanceMethod(IntrazonalDistanceMethod.AREA);
    copy.setNumberOfNearestZones(5);
    copy.setNearestZonesFactor(0.25);
    copy.setStraightLineSpeedKmhFor(car, 30);

    assertEquals(25.0, settings.getConnectoidSpeedKmh());
    assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() - 1), settings.getNumberOfThreads());
    assertEquals(taxi, settings.getProxyModeFor(taxi));
    assertFalse(settings.isInferUnreachableCells());
    assertEquals(1.3, settings.getDetourFactor());
    assertEquals(IntrazonalDistanceMethod.NEAREST_NEIGHBOURS, settings.getIntrazonalDistanceMethod());
    assertEquals(3, settings.getNumberOfNearestZones());
    assertEquals(0.5, settings.getNearestZonesFactor());
    assertTrue(Double.isNaN(settings.getStraightLineSpeedKmhFor(car)));
    assertEquals(5.0, copy.getConnectoidSpeedKmh());
    assertEquals(1, copy.getNumberOfThreads());
    assertEquals(car, copy.getProxyModeFor(taxi));
    assertEquals(30.0, copy.getStraightLineSpeedKmhFor(car));
  }
}
