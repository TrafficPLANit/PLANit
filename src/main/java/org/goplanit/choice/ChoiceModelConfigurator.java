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

  public static final String SET_SCALING_FACTOR = "setScalingFactor";

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public ChoiceModelConfigurator(Class<T> instanceType) {
    super(instanceType);
  }


  /**
   * Return the scale parameter
   *
   * @return the scale parameter
   */
  public double getScalingFactor() {
    return (double) getFirstParameterOfDelayedMethodCall(SET_SCALING_FACTOR);
  }

  /**
   * Set the scale parameter
   *
   * @param scalingFactor the scale factor
   */
  public void setScalingFactor(double scalingFactor) {
    registerDelayedMethodCall(SET_SCALING_FACTOR, scalingFactor);
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
