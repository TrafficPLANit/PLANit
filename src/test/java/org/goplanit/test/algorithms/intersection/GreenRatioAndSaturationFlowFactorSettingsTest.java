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

  /** A value that makes no sense is refused and leaves the setting as it was; a sensible one is set */
  @Test
  public void valuesThatMakeNoSenseAreRefused() {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();

    assertFalse(settings.setCycleTimeSeconds(0));
    assertFalse(settings.setCycleTimeSeconds(-90));
    assertFalse(settings.setLostTimePerPhaseChangeSeconds(-1));
    assertFalse(settings.setIsolatedCrossingGreenRatio(1.2));
    assertFalse(settings.setRampMeterGreenRatio(1.2));
    assertFalse(settings.setRampMeterGreenRatio(-0.1));
    assertFalse(settings.setApproachWithoutSignalSaturationFlowFactor(-0.1));
    assertFalse(settings.setSameAxisApproachToleranceDegrees(90));
    assertFalse(settings.setMaximumNumberOfPhases(0));
    assertFalse(settings.setAlgorithmType(null));
    assertFalse(settings.setFixedRailBasedPhaseGreenRatio(-0.1));
    assertFalse(settings.setFixedRailBasedPhaseGreenRatio(1.2));
    assertFalse(settings.setMinimumGreenTimeSeconds(-1));
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

    assertTrue(settings.setCycleTimeSeconds(120));
    assertTrue(settings.setMaximumNumberOfPhases(3));
    assertEquals(3, settings.getMaximumNumberOfPhases());
    assertTrue(settings.setAlgorithmType(GreenRatioAlgorithmType.EQUAL_SPLIT));
    assertEquals(120, settings.getCycleTimeSeconds());
    assertEquals(GreenRatioAlgorithmType.EQUAL_SPLIT, settings.getAlgorithmType());
    assertTrue(settings.setFixedRailBasedPhaseGreenRatio(0));
    assertTrue(settings.setMinimumGreenTimeSeconds(8));
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
