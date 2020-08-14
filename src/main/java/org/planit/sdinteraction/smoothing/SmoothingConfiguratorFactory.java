package org.planit.sdinteraction.smoothing;

import org.planit.utils.exceptions.PlanItException;

/**
 * Traffic assignment builder factory for the smoothing types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class SmoothingConfiguratorFactory {

  /**
   * Create a configurator for given smoothing type
   * 
   * @param smoothingType   type of assignment the builder is created for
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
