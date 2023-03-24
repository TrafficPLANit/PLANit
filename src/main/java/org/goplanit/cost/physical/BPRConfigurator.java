package org.goplanit.cost.physical;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Configurator for BPRLinkTravelTimeCost implementation
 * 
 * @author markr
 */
public class BPRConfigurator extends PhysicalCostConfigurator<BprLinkTravelTimeCost> {

  private static final String SET_PARAMETERS = "setParameters";

  private static final String SET_DEFAULT_PARAMETERS = "setDefaultParameters";

  /**
   * Constructor
   * 
   */
  protected BPRConfigurator() {
    super(BprLinkTravelTimeCost.class);
  }

  /**
   * Set the alpha and beta values for a given link segment and mode
   *
   * @param linkSegment the specified link segment
   * @param mode        specified mode type
   * @param alpha       alpha value
   * @param beta        beta value
   */
  public void setParameters(final MacroscopicLinkSegment linkSegment, final Mode mode, final double alpha, final double beta) {
    registerDelayedMethodCall(SET_PARAMETERS, linkSegment, mode, alpha, beta);
  }

  /**
   * Set the default alpha and beta values for a mode
   *
   * @param mode  the specified mode type
   * @param alpha alpha value
   * @param beta  beta value
   */
  public void setDefaultParameters(final Mode mode, final double alpha, final double beta) {
    registerDelayedMethodCall(SET_DEFAULT_PARAMETERS, mode, alpha, beta);
  }

  /**
   * Set the default alpha and beta values for a given link type and mode
   *
   * @param macroscopicLinkSegmentType the specified link type
   * @param mode                       the specified mode type
   * @param alpha                      alpha value
   * @param beta                       beta value
   */
  public void setDefaultParameters(final MacroscopicLinkSegmentType macroscopicLinkSegmentType, final Mode mode, final double alpha, final double beta) {
    registerDelayedMethodCall(SET_DEFAULT_PARAMETERS, macroscopicLinkSegmentType, mode, alpha, beta);
  }

  /**
   * Set the default alpha and beta values
   *
   * @param alpha alpha value
   * @param beta  beta value
   */
  public void setDefaultParameters(final double alpha, final double beta) {
    registerDelayedMethodCall(SET_DEFAULT_PARAMETERS, alpha, beta);
  }

}
