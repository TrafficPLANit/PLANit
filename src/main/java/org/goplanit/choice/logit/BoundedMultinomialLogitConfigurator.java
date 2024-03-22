package org.goplanit.choice.logit;

import org.goplanit.choice.ChoiceModelConfigurator;

/**
 * Bounded MNL configurator implementations
 * 
 * @author markr
 *
 */
public class BoundedMultinomialLogitConfigurator extends ChoiceModelConfigurator<BoundedMultinomialLogit> {

  protected static final String SET_DELTA = "setDelta";

  /**
   * Constructor
   *
   */
  public BoundedMultinomialLogitConfigurator() {
    super(BoundedMultinomialLogit.class);
  }

   /** Delta determines the bounds of the model, i.e., when delta is 1 and reference cost (best alternative) is 4 then
   * probabilities will range from 0-1 in domain [4-1,4+1]. Outside of this domain, probabilities will always be 0
   *
   * @param delta the delta to apply
   */
  public void setDelta(final double delta) {
    registerDelayedMethodCall(SET_DELTA, delta);
  }
}
