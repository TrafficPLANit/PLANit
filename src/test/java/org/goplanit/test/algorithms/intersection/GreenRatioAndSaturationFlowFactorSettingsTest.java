package org.goplanit.test.algorithms.intersection;

import org.goplanit.algorithms.intersection.GreenRatioAlgorithmType;
import org.goplanit.algorithms.intersection.GreenRatioAndSaturationFlowFactorSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the settings of the algorithms computing green ratios and saturation flow factors: their defaults, the values
 * refused, and copies
 *
 * @author markr
 */
public class GreenRatioAndSaturationFlowFactorSettingsTest {

  /** The defaults: equal split, a cycle of 90 s, 5 s lost per phase change, 0.9 at an isolated crossing, 30 degrees for
   * opposite approaches and 0.5 for an approach without a signal of its own */
  @Test
  public void defaults() {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();

    assertEquals(GreenRatioAlgorithmType.EQUAL_SPLIT, settings.getAlgorithmType());
    assertEquals(90, settings.getCycleTimeSeconds());
    assertEquals(5, settings.getLostTimePerPhaseChangeSeconds());
    assertEquals(0.9, settings.getIsolatedCrossingGreenRatio());
    assertEquals(30, settings.getOppositeApproachToleranceDegrees());
    assertEquals(0.5, settings.getApproachWithoutSignalSaturationFlowFactor());
  }

  /** A value that makes no sense is refused and leaves the setting as it was; a sensible one is set */
  @Test
  public void valuesThatMakeNoSenseAreRefused() {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();

    assertFalse(settings.setCycleTimeSeconds(0));
    assertFalse(settings.setCycleTimeSeconds(-90));
    assertFalse(settings.setLostTimePerPhaseChangeSeconds(-1));
    assertFalse(settings.setIsolatedCrossingGreenRatio(1.2));
    assertFalse(settings.setApproachWithoutSignalSaturationFlowFactor(-0.1));
    assertFalse(settings.setOppositeApproachToleranceDegrees(90));
    assertFalse(settings.setAlgorithmType(null));
    assertEquals(90, settings.getCycleTimeSeconds());
    assertEquals(5, settings.getLostTimePerPhaseChangeSeconds());
    assertEquals(0.9, settings.getIsolatedCrossingGreenRatio());
    assertEquals(0.5, settings.getApproachWithoutSignalSaturationFlowFactor());
    assertEquals(30, settings.getOppositeApproachToleranceDegrees());
    assertEquals(GreenRatioAlgorithmType.EQUAL_SPLIT, settings.getAlgorithmType());

    assertTrue(settings.setCycleTimeSeconds(120));
    assertTrue(settings.setAlgorithmType(GreenRatioAlgorithmType.CAPACITY_WEIGHTED));
    assertEquals(120, settings.getCycleTimeSeconds());
    assertEquals(GreenRatioAlgorithmType.CAPACITY_WEIGHTED, settings.getAlgorithmType());
  }

  /** A copy holds the same values, and changing it leaves the original as it was */
  @Test
  public void copyHoldsTheSameValuesOnItsOwn() {
    var original = new GreenRatioAndSaturationFlowFactorSettings();
    original.setCycleTimeSeconds(100);

    var copy = new GreenRatioAndSaturationFlowFactorSettings(original);
    assertEquals(100, copy.getCycleTimeSeconds());

    copy.setCycleTimeSeconds(60);
    assertEquals(100, original.getCycleTimeSeconds());
  }
}
