package org.goplanit.choice;

import org.goplanit.choice.logit.BoundedMultinomialLogit;
import org.goplanit.choice.logit.BoundedMultinomialLogitConfigurator;
import org.goplanit.choice.logit.MultinomialLogitConfigurator;
import org.goplanit.choice.weibit.WeibitConfigurator;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

/**
 * factory for the choice model types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class ChoiceModelConfiguratorFactory {

  /**
   * Create a configurator for given choice model type
   * 
   * @param choiceModelType   type of assignment the builder is created for
   * @return the created configurator
   */
  public static ChoiceModelConfigurator<? extends ChoiceModel> createConfigurator(final String choiceModelType) {

    if (choiceModelType.equals(ChoiceModel.MNL)) {
      return new MultinomialLogitConfigurator();
    }else if (choiceModelType.equals(ChoiceModel.WEIBIT)) {
      return new WeibitConfigurator();
    }else if (choiceModelType.equals(ChoiceModel.BOUNDED_MNL)) {
      return new BoundedMultinomialLogitConfigurator();
    }else {
      throw new PlanItRunTimeException("unable to construct configurator for given choice model Type %s", choiceModelType);
    }
  }
}
