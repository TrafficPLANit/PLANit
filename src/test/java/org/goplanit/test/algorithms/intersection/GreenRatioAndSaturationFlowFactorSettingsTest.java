package org.goplanit.test.algorithms.intersection;

import org.goplanit.algorithms.intersection.GreenRatioAlgorithmType;
import org.goplanit.algorithms.intersection.GreenRatioAndSaturationFlowFactorSettings;
import org.goplanit.logging.Logging;
import org.goplanit.utils.settings.SettingsFileContentType;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.settings.SettingsYamlLoader;
import org.goplanit.utils.settings.SettingsYamlWriter;
import org.goplanit.utils.settings.annotations.SettingsHeader;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsSection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the settings of the algorithms computing green ratios and saturation flow factors: their defaults, reading
 * them from a file and writing them out, the values refused, and copies
 *
 * @author markr
 */
public class GreenRatioAndSaturationFlowFactorSettingsTest {

  /** Settings holding the green ratio settings as a section, as the settings of a converter do */
  @SettingsHeader(key = "planitGreenRatioTestSettings", version = 1)
  @SettingsSection(key = "test", level = SettingsKeyLevel.STANDARD,
      description = "Settings holding the green ratio settings")
  public static final class TestGreenRatioHolderSettings {

    /** the green ratio settings held */
    private final GreenRatioAndSaturationFlowFactorSettings greenRatioSettings =
        new GreenRatioAndSaturationFlowFactorSettings();

    /** @return the green ratio settings held */
    @SettingsSection(key = "greenRatioAndSaturationFlowFactor", level = SettingsKeyLevel.UNCOMMON,
        description = "How the green ratio and saturation flow factor of each signalised turn are worked out")
    public GreenRatioAndSaturationFlowFactorSettings getGreenRatioAndSaturationFlowFactorSettings() {
      return greenRatioSettings;
    }
  }

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
      LOGGER = Logging.createLogger(GreenRatioAndSaturationFlowFactorSettingsTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  /** The defaults: capacity weighted, a cycle of 90 s, 5 s lost per phase change, 0.9 at an isolated crossing and at a
   * ramp meter, 30 degrees for approaches on the same axis, at most four phases, 0.5 for an approach without a signal
   * of its own, 0.1 for the rail-based phase and a minimum green time of 5 s */
  @Test
  public void defaults() {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();

    assertEquals(GreenRatioAlgorithmType.CAPACITY_WEIGHTED, settings.getAlgorithmType());
    assertEquals(90, settings.getCycleTimeSeconds());
    assertEquals(5, settings.getLostTimePerPhaseChangeSeconds());
    assertEquals(0.9, settings.getIsolatedCrossingGreenRatio());
    assertEquals(0.9, settings.getRampMeterGreenRatio());
    assertEquals(30, settings.getSameAxisApproachToleranceDegrees());
    assertEquals(4, settings.getMaximumNumberOfPhases());
    assertEquals(0.5, settings.getApproachWithoutSignalSaturationFlowFactor());
    assertEquals(0.1, settings.getFixedRailBasedPhaseGreenRatio());
    assertEquals(5, settings.getMinimumGreenTimeSeconds());
  }

  /**
   * Every value of the green ratio settings read back from a file, each away from its default, and written out again
   * as the keys differing from the defaults, the common key first and then the uncommon ones by name
   */
  @Test
  public void everyValueReadsBackFromAFileAndIsWrittenOut() {
    var schema = new SettingsSchema(TestGreenRatioHolderSettings.class);
    var holder = new SettingsYamlLoader(schema, List.of(SettingsSource.ofText(schema,
        "test:\n  greenRatioAndSaturationFlowFactor:\n    algorithmType: EQUAL_SPLIT\n    cycleTimeSeconds: 100\n" +
            "    lostTimePerPhaseChangeSeconds: 4\n    isolatedCrossingGreenRatio: 0.8\n" +
            "    rampMeterGreenRatio: 0.6\n    sameAxisApproachToleranceDegrees: 20\n    maximumNumberOfPhases: 3\n" +
            "    approachWithoutSignalSaturationFlowFactor: 0.4\n    fixedRailBasedPhaseGreenRatio: 0.15\n" +
            "    minimumGreenTimeSeconds: 7\n")))
        .create(TestGreenRatioHolderSettings.class);
    var settings = holder.getGreenRatioAndSaturationFlowFactorSettings();

    assertEquals(GreenRatioAlgorithmType.EQUAL_SPLIT, settings.getAlgorithmType());
    assertEquals(100, settings.getCycleTimeSeconds());
    assertEquals(4, settings.getLostTimePerPhaseChangeSeconds());
    assertEquals(0.8, settings.getIsolatedCrossingGreenRatio());
    assertEquals(0.6, settings.getRampMeterGreenRatio());
    assertEquals(20, settings.getSameAxisApproachToleranceDegrees());
    assertEquals(3, settings.getMaximumNumberOfPhases());
    assertEquals(0.4, settings.getApproachWithoutSignalSaturationFlowFactor());
    assertEquals(0.15, settings.getFixedRailBasedPhaseGreenRatio());
    assertEquals(7, settings.getMinimumGreenTimeSeconds());

    assertEquals("planitGreenRatioTestSettings: 1\n"
            + "test:\n"
            + "  greenRatioAndSaturationFlowFactor:\n"
            + "    algorithmType: EQUAL_SPLIT\n"
            + "    approachWithoutSignalSaturationFlowFactor: 0.4\n"
            + "    cycleTimeSeconds: 100.0\n"
            + "    fixedRailBasedPhaseGreenRatio: 0.15\n"
            + "    isolatedCrossingGreenRatio: 0.8\n"
            + "    lostTimePerPhaseChangeSeconds: 4.0\n"
            + "    maximumNumberOfPhases: 3\n"
            + "    minimumGreenTimeSeconds: 7.0\n"
            + "    rampMeterGreenRatio: 0.6\n"
            + "    sameAxisApproachToleranceDegrees: 20.0\n",
        new SettingsYamlWriter(schema, holder, SettingsFileContentType.KEYS_DIFFERING_FROM_DEFAULTS, false,
            Path.of("settings.yaml")).toString());
  }

  /** A value that makes no sense is refused and leaves the setting as it was; a sensible one is set */
  @Test
  public void valuesThatMakeNoSenseAreRefused() {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();

    assertThrows(IllegalArgumentException.class, () -> settings.setCycleTimeSeconds(0));
    assertThrows(IllegalArgumentException.class, () -> settings.setCycleTimeSeconds(-90));
    assertThrows(IllegalArgumentException.class, () -> settings.setLostTimePerPhaseChangeSeconds(-1));
    assertThrows(IllegalArgumentException.class, () -> settings.setIsolatedCrossingGreenRatio(1.2));
    assertThrows(IllegalArgumentException.class, () -> settings.setRampMeterGreenRatio(1.2));
    assertThrows(IllegalArgumentException.class, () -> settings.setRampMeterGreenRatio(-0.1));
    assertThrows(IllegalArgumentException.class, () -> settings.setApproachWithoutSignalSaturationFlowFactor(-0.1));
    assertThrows(IllegalArgumentException.class, () -> settings.setSameAxisApproachToleranceDegrees(90));
    assertThrows(IllegalArgumentException.class, () -> settings.setMaximumNumberOfPhases(0));
    assertThrows(IllegalArgumentException.class, () -> settings.setAlgorithmType(null));
    assertThrows(IllegalArgumentException.class, () -> settings.setFixedRailBasedPhaseGreenRatio(-0.1));
    assertThrows(IllegalArgumentException.class, () -> settings.setFixedRailBasedPhaseGreenRatio(1.2));
    assertThrows(IllegalArgumentException.class, () -> settings.setMinimumGreenTimeSeconds(-1));
    assertEquals(90, settings.getCycleTimeSeconds());
    assertEquals(5, settings.getLostTimePerPhaseChangeSeconds());
    assertEquals(0.9, settings.getIsolatedCrossingGreenRatio());
    assertEquals(0.9, settings.getRampMeterGreenRatio());
    assertEquals(0.5, settings.getApproachWithoutSignalSaturationFlowFactor());
    assertEquals(30, settings.getSameAxisApproachToleranceDegrees());
    assertEquals(4, settings.getMaximumNumberOfPhases());
    assertEquals(GreenRatioAlgorithmType.CAPACITY_WEIGHTED, settings.getAlgorithmType());
    assertEquals(0.1, settings.getFixedRailBasedPhaseGreenRatio());
    assertEquals(5, settings.getMinimumGreenTimeSeconds());

    settings.setCycleTimeSeconds(120);
    settings.setMaximumNumberOfPhases(3);
    assertEquals(3, settings.getMaximumNumberOfPhases());
    settings.setAlgorithmType(GreenRatioAlgorithmType.EQUAL_SPLIT);
    assertEquals(120, settings.getCycleTimeSeconds());
    assertEquals(GreenRatioAlgorithmType.EQUAL_SPLIT, settings.getAlgorithmType());
    settings.setFixedRailBasedPhaseGreenRatio(0);
    settings.setMinimumGreenTimeSeconds(8);
    assertEquals(0, settings.getFixedRailBasedPhaseGreenRatio());
    assertEquals(8, settings.getMinimumGreenTimeSeconds());
  }

  /** A copy holds the same values, and changing it leaves the original as it was */
  @Test
  public void copyHoldsTheSameValuesOnItsOwn() {
    var original = new GreenRatioAndSaturationFlowFactorSettings();
    original.setCycleTimeSeconds(100);
    original.setMaximumNumberOfPhases(3);
    original.setFixedRailBasedPhaseGreenRatio(0.15);
    original.setMinimumGreenTimeSeconds(7);
    original.setRampMeterGreenRatio(0.6);

    var copy = new GreenRatioAndSaturationFlowFactorSettings(original);
    assertEquals(100, copy.getCycleTimeSeconds());
    assertEquals(3, copy.getMaximumNumberOfPhases());
    assertEquals(0.15, copy.getFixedRailBasedPhaseGreenRatio());
    assertEquals(7, copy.getMinimumGreenTimeSeconds());
    assertEquals(0.6, copy.getRampMeterGreenRatio());

    copy.setCycleTimeSeconds(60);
    assertEquals(100, original.getCycleTimeSeconds());
  }
}
