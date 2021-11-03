package org.goplanit.path.choice.logit;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all logit choice model configurator implementations
 * 
 * @author markr
 *
 * @param <T> logit choice model type
 */
public class LogitChoiceModelConfigurator<T extends LogitChoiceModel> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public LogitChoiceModelConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends logit choice model
   * 
   * @param logitChoiceModel the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(LogitChoiceModel logitChoiceModel) throws PlanItException {
    super.configure((T) logitChoiceModel);
  }
}
