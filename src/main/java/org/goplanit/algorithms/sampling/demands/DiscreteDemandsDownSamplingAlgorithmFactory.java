package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

/**
 * Creates the down-sampling algorithm of the quality the settings choose, the quality being how the sampling units are
 * chosen, from households at random to the best of several balanced draws, see
 * {@link DiscreteDemandsDownSamplingAlgorithmType}
 * <p>
 * Each algorithm owns a copy of the settings, so the settings can be changed afterwards, for the next draw with
 * another seed for example, without changing an algorithm created before.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingAlgorithmFactory {

  /** static factory, not to be instantiated */
  private DiscreteDemandsDownSamplingAlgorithmFactory() {
  }

  /**
   * Create the algorithm of the settings' type, with a copy of the settings
   *
   * @param settings to use
   * @return the algorithm
   * @throws PlanItRunTimeException when the settings' type has no algorithm
   */
  public static DiscreteDemandsDownSamplingAlgorithm create(DiscreteDemandsDownSamplingSettings settings) {
    var copiedSettings = settings.shallowClone();
    switch (copiedSettings.getAlgorithmType()) {
      case HOUSEHOLDS_AT_RANDOM:
        return new RandomHouseholdsDownSamplingAlgorithm(copiedSettings);
      case BY_ZONE:
        return new ByZoneDownSamplingAlgorithm(copiedSettings);
      case BALANCED:
        return new BalancedDownSamplingAlgorithm(copiedSettings);
      case BALANCED_BEST_OF_SEVERAL:
        return new BestOfSeveralBalancedDownSamplingAlgorithm(copiedSettings);
      default:
        throw new PlanItRunTimeException(
            "Down-sampling algorithm type %s is not available", copiedSettings.getAlgorithmType());
    }
  }
}
