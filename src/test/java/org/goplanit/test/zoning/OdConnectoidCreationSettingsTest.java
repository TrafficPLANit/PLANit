package org.goplanit.test.zoning;

import org.goplanit.converter.zoning.connectoid.OdConnectoidAttachment;
import org.goplanit.converter.zoning.connectoid.OdConnectoidCreationSettings;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings of creating OD connectoids: their defaults, the values refused, and a copy apart from the original
 *
 * @author markr
 */
public class OdConnectoidCreationSettingsTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(OdConnectoidCreationSettingsTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  /** Every setting starts at its default */
  @Test
  public void testDefaults() {
    var settings = new OdConnectoidCreationSettings();
    assertTrue(settings.getModes().isEmpty());
    assertEquals(1, settings.getNumberOfConnectoidsPerZone());
    assertEquals(1.0, settings.getPreferredDistanceZoneRadiusFactor());
    assertEquals(5000.0, settings.getMaximumDistanceMetres());
    assertEquals(OdConnectoidAttachment.EXISTING_NODE, settings.getAttachment());
    assertEquals(0.1, settings.getMinimumDistanceToNodeFactor());
    assertEquals(60.0, settings.getMaximumLinkSpeedKmh());
    assertTrue(settings.isMainNetworkOnly());
    assertFalse(settings.isReplaceExistingConnectoids());
  }

  /** Each value that cannot be used is refused, and the setting keeps the value it had */
  @Test
  public void testRefusals() {
    var settings = new OdConnectoidCreationSettings();
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfConnectoidsPerZone(0));
    assertEquals(1, settings.getNumberOfConnectoidsPerZone());
    assertThrows(IllegalArgumentException.class, () -> settings.setPreferredDistanceZoneRadiusFactor(0));
    assertEquals(1.0, settings.getPreferredDistanceZoneRadiusFactor());
    assertThrows(IllegalArgumentException.class, () -> settings.setMaximumDistanceMetres(-1));
    assertEquals(5000.0, settings.getMaximumDistanceMetres());
    assertThrows(IllegalArgumentException.class, () -> settings.setMinimumDistanceToNodeFactor(0));
    assertEquals(0.1, settings.getMinimumDistanceToNodeFactor());
    assertThrows(IllegalArgumentException.class, () -> settings.setMaximumLinkSpeedKmh(0));
    assertEquals(60.0, settings.getMaximumLinkSpeedKmh());
    assertThrows(IllegalArgumentException.class, settings::setModes);
    assertTrue(settings.getModes().isEmpty());
  }

  /** A copy can be changed without changing the original */
  @Test
  public void testShallowCloneApart() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var settings = new OdConnectoidCreationSettings();
    var copy = settings.shallowClone();
    copy.setNumberOfConnectoidsPerZone(3);
    copy.setAttachment(OdConnectoidAttachment.NEW_NODE_ON_LINK);
    copy.setModes(car);
    copy.setMainNetworkOnly(false);
    assertEquals(1, settings.getNumberOfConnectoidsPerZone());
    assertEquals(OdConnectoidAttachment.EXISTING_NODE, settings.getAttachment());
    assertTrue(settings.getModes().isEmpty());
    assertTrue(settings.isMainNetworkOnly());
    assertEquals(3, copy.getNumberOfConnectoidsPerZone());
    assertEquals(Set.of(car), copy.getModes());
  }
}
