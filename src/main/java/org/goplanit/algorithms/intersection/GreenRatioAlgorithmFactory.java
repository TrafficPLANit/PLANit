package org.goplanit.algorithms.intersection;

import org.goplanit.utils.exceptions.PlanItRunTimeException;

/**
 * Creates the algorithm computing green ratios and saturation flow factors that the settings choose
 *
 * @author markr
 */
public class GreenRatioAlgorithmFactory {

  /** static factory, not to be instantiated */
  private GreenRatioAlgorithmFactory() {
  }

  /**
   * Create the algorithm of the settings' type, with a copy of the settings
   *
   * @param settings to use
   * @return the algorithm
   */
  public static GreenRatioAlgorithm create(GreenRatioAndSaturationFlowFactorSettings settings) {
    var copiedSettings = new GreenRatioAndSaturationFlowFactorSettings(settings);
    switch (copiedSettings.getAlgorithmType()) {
      case EQUAL_SPLIT:
        return new EqualSplitGreenRatioAlgorithm(copiedSettings);
      case CAPACITY_WEIGHTED:
        return new CapacityWeightedGreenRatioAlgorithm(copiedSettings);
      default:
        throw new PlanItRunTimeException("Unknown green ratio algorithm type %s", copiedSettings.getAlgorithmType());
    }
  }
}
