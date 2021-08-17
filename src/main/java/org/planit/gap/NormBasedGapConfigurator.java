package org.planit.gap;

/**
 * Norm based gap function configurator
 * 
 * @author markr
 *
 */
public class NormBasedGapConfigurator extends GapFunctionConfigurator<NormBasedGapFunction> {

  /** delayed call to apply for setting averaged option on gap function */
  protected static final String SET_AVERAGED = "setAveraged";

  /** delayed call to apply for setting norm option on gap function */
  protected static final String SET_NORM = "setNorm";

  /**
   * Constructor
   * 
   */
  public NormBasedGapConfigurator() {
    super(NormBasedGapFunction.class);
  }

  /**
   * Set the averaged property indicating to average the gap after computation or not
   * 
   * @param averaged when true it is averaged, false otherwise
   */
  public void setAveraged(boolean averaged) {
    registerDelayedMethodCall(SET_AVERAGED, averaged);
  }

  /**
   * Set the norm property indicating what norm to apply when computing the gap
   * 
   * @param norm to apply
   */
  public void setNorm(int norm) {
    registerDelayedMethodCall(SET_NORM, norm);
  }

}
