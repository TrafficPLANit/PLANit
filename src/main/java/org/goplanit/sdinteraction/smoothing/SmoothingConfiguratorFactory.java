package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

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
   */
  public static SmoothingConfigurator<? extends Smoothing> createConfigurator(final String smoothingType){

    if (smoothingType.equals(Smoothing.MSA)) {
      return new MSASmoothingConfigurator();
    } else if (smoothingType.equals(Smoothing.FIXED_STEP)) {
      return new FixedStepSmoothingConfigurator();
    }else {
      throw new PlanItRunTimeException(String.format("Unable to construct configurator for given smoothingType %s", smoothingType));
    }
  }
}
