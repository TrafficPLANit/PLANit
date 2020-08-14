package org.planit.path.choice.logit;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all logit choice model configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class LogitChoiceModelConfigurator<T extends LogitChoiceModel> extends Configurator<T> {

  /**
   * Constructor 
   * @param instanceType to configure on
   */
  public LogitChoiceModelConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends logit choice model
   * 
   * @param logit choice model the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(LogitChoiceModel logitChoiceModel) throws PlanItException {
    super.configure((T) logitChoiceModel);
  }
}
