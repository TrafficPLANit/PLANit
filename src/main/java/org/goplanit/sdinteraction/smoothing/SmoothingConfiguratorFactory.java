package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * factory for the smoothing configurator types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class SmoothingConfiguratorFactory {

  /**
   * Create a configurator for given smoothing type
   * 
   * @param smoothingType type of assignment the builder is created for
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static SmoothingConfigurator<? extends Smoothing> createConfigurator(final String smoothingType) throws PlanItException {

    if (smoothingType.equals(Smoothing.MSA)) {
      return new MSASmoothingConfigurator();
    } else {
      throw new PlanItException(String.format("unable to construct configurator for given smoothingType %s", smoothingType));
    }
  }
}
