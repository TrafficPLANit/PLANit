package org.planit.sdinteraction.smoothing;

import org.planit.utils.exceptions.PlanItException;

/**
 * builder for MSA smoothing implementation
 * 
 * @author markr
 */
public class MSASmoothingBuilder extends SmoothingBuilder<MSASmoothing> {

  /**
   * Constructor
   * 
   */
  protected MSASmoothingBuilder() {
    super(MSASmoothing.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MSASmoothing build() throws PlanItException {
    MSASmoothing msaSmoothing = super.build();
    invokeDelayedMethodCalls(msaSmoothing);
    return msaSmoothing;
  }

}
