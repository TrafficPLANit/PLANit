package org.goplanit.choice;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all choice model configurator implementations
 * 
 * @author markr
 *
 * @param <T> choice model type
 */
public class ChoiceModelConfigurator<T extends ChoiceModel> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public ChoiceModelConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends logit choice model
   * 
   * @param choiceModel the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(ChoiceModel choiceModel){
    super.configure((T) choiceModel);
  }
}
