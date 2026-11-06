package org.goplanit.path.choice;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

/**
 * factory for the path choice types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class PathChoiceConfiguratorFactory {

  /**
   * Create a configurator for given path choicetype
   * 
   * @param pathChoiceType   type of assignment the builder is created for
   * @return the created configurator
   */
  public static PathChoiceConfigurator<? extends PathChoice> createConfigurator(final String pathChoiceType){

    if (pathChoiceType.equals(PathChoice.STOCHASTIC)) {
      return new StochasticPathChoiceConfigurator();
    }else {
      throw new PlanItRunTimeException(
          String.format("Unable to construct configurator for given pathChoiceType %s", pathChoiceType));
    }
  }
}
