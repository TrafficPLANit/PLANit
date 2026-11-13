package org.goplanit.choice.logit;

import org.goplanit.choice.ChoiceModelConfigurator;

/**
 * MNL configurator implementations
 * 
 * @author markr
 *
 */
public class MultinomialLogitConfigurator extends ChoiceModelConfigurator<MultinomialLogit> {

  /**
   * Constructor
   * 
   */
  public MultinomialLogitConfigurator() {
    super(MultinomialLogit.class);
  }
}
