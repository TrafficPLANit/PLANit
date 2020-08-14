package org.planit.path.choice.logit;

/**
 * MNL configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class MultinomialLogitConfigurator extends LogitChoiceModelConfigurator<MultinomialLogit> {

  /**
   * Constructor 
   * @param instanceType to configure on
   */
  public MultinomialLogitConfigurator() {
    super(MultinomialLogit.class);
  }
}
