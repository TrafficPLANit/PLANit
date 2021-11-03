package org.goplanit.path.choice.logit;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * factory for the logit choice model types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class LogitChoiceModelConfiguratorFactory {

  /**
   * Create a configurator for given logit choice model type
   * 
   * @param logitChoiceModelType   type of assignment the builder is created for
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static LogitChoiceModelConfigurator<? extends LogitChoiceModel> createConfigurator(final String logitChoiceModelType) throws PlanItException {

    if (logitChoiceModelType.equals(LogitChoiceModel.MNL)) {
      return new MultinomialLogitConfigurator();
    }else {
      throw new PlanItException(String.format("unable to construct configurator for given logit model Type %s", logitChoiceModelType));
    }
  }
}
