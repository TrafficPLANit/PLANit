package org.goplanit.test.converter.zoning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.goplanit.converter.zoning.AccessEgressInjectionSettings;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.settings.SettingsYamlLoader;
import org.goplanit.utils.settings.annotations.SettingsHeader;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsSection;
import org.junit.jupiter.api.Test;

/**
 * Tests the settings of connecting stops to the network for access and egress: every value read from a file, and the
 * values refused
 * <p>
 * A converter holds these settings as a section of its own settings, so they are read here through a test section
 * </p>
 *
 * @author markr
 */
public class AccessEgressInjectionSettingsTest {

  /** Settings holding the access and egress settings as a section, as the settings of a converter do */
  @SettingsHeader(key = "planitAccessEgressTestSettings", version = 1)
  @SettingsSection(key = "test", level = SettingsKeyLevel.STANDARD,
      description = "Settings holding the access and egress settings")
  public static final class TestAccessEgressHolderSettings {

    /** the access and egress settings held */
    private final AccessEgressInjectionSettings accessEgressSettings = new AccessEgressInjectionSettings();

    /** @return the access and egress settings held */
    @SettingsSection(key = "accessEgressInjection", level = SettingsKeyLevel.UNCOMMON, description =
        "Linking stops to the nearby network so passengers can reach them, such as ferry stops to the land network")
    public AccessEgressInjectionSettings getAccessEgressInjectionSettings() {
      return accessEgressSettings;
    }
  }

  /**
   * Every value read back from a file, each away from its default: the four flags off, all on by default, and the
   * radii of 300, 150 and 40 m instead of 500, 200 and 25 m
   */
  @Test
  public void everyValueReadsBackFromAFile() {
    var schema = new SettingsSchema(TestAccessEgressHolderSettings.class);
    var settings = new SettingsYamlLoader(schema, List.of(SettingsSource.ofText(schema,
        "test:\n  accessEgressInjection:\n    connectFerryStopsToNearbyLandNetwork: false\n" +
            "    ferryStopToLandNetworkSearchRadiusMeters: 300\n    connectRailBasedStopsToPassengerNetwork: false\n" +
            "    railBasedStopToPassengerNetworkSearchRadiusMeters: 150\n" +
            "    connectBusBasedStopsToPassengerNetwork: false\n" +
            "    busBasedStopToPassengerNetworkSearchRadiusMeters: 40\n" +
            "    fallbackOnRoadModesWithoutActiveModes: false\n")))
        .create(TestAccessEgressHolderSettings.class).getAccessEgressInjectionSettings();

    assertFalse(settings.isConnectFerryStopsToNearbyLandNetwork());
    assertEquals(300, settings.getFerryStopToLandNetworkSearchRadiusMeters());
    assertFalse(settings.isConnectRailBasedStopsToPassengerNetwork());
    assertEquals(150, settings.getRailBasedStopToPassengerNetworkSearchRadiusMeters());
    assertFalse(settings.isConnectBusBasedStopsToPassengerNetwork());
    assertEquals(40, settings.getBusBasedStopToPassengerNetworkSearchRadiusMeters());
    assertFalse(settings.isFallbackOnRoadModesWithoutActiveModes());
  }

  /** A search radius not above 0 m finds nothing, so it is refused and the radius kept as it was */
  @Test
  public void searchRadiusNotAboveZeroIsRefused() {
    var settings = new AccessEgressInjectionSettings();

    assertThrows(IllegalArgumentException.class, () -> settings.setFerryStopToLandNetworkSearchRadiusMeters(0));
    assertThrows(IllegalArgumentException.class, () -> settings.setRailBasedStopToPassengerNetworkSearchRadiusMeters(0));
    assertThrows(IllegalArgumentException.class, () -> settings.setBusBasedStopToPassengerNetworkSearchRadiusMeters(-1));
    assertEquals(500, settings.getFerryStopToLandNetworkSearchRadiusMeters());
    assertEquals(200, settings.getRailBasedStopToPassengerNetworkSearchRadiusMeters());
    assertEquals(25, settings.getBusBasedStopToPassengerNetworkSearchRadiusMeters());
  }
}
