package org.planit.path.choice;

import org.planit.utils.exceptions.PlanItException;

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
   * @throws PlanItException thrown if error
   */
  public static PathChoiceConfigurator<? extends PathChoice> createConfigurator(final String pathChoiceType) throws PlanItException {

    if (pathChoiceType.equals(PathChoice.STOCHASTIC)) {
      return new StochasticPathChoiceConfigurator();
    }else {
      throw new PlanItException(String.format("unable to construct configurator for given pathChoiceType %s", pathChoiceType));
    }
  }
}
