package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Configurator for Quadratic-Linear fundamental diagram implementation. We allow one to overwrite the capacity and
 * maximum densities used for each FD but not the free speed, since the free speed is a physical value that is a given.
 * Note that a change on the link segment level takes precedence over a change on the link segment type, i.e., the most specific
 * overwrite is used in the final fundamental diagram applied on the link segment.
 * <p>
 * In absence of a capacity, the capacity is computed by the point of intersection of the free flow branch and
 * congested branch, which for Newell is defined by only the free flow speed, maximum density and intersection points
 * with the x-axis (density=0), by explicitly setting the capacity or moving the maximum density point the FD will adjust its
 * backward wave speed to accommodate any change compared to the default computed capacity/maximum density.
 * </p>
 * <p>
 *   For QL there is an option to force concavity (Default activated) even if the critical speed is not defined (in
 *   which case a percentage of free speed is applied (also configurable)). When critical speed is defined this speed is
 *   assuming it is smaller than the free speed.
 * </p>
 * 
 * @author markr
 */
public class QuadraticLinearFundamentalDiagramConfigurator extends FundamentalDiagramConfigurator<QuadraticLinearFundamentalDiagramComponent> {

  private static final String SET_CAPACITY_LINK_SEGMENT = "setCapacityLinkSegmentPcuHourLane";

  private static final String SET_MAXIMUM_DENSITY_LINK_SEGMENT = "setMaximumDensityLinkSegmentPcuKmLane";

  private static final String SET_CAPACITY_LINK_SEGMENT_TYPE = "setCapacityLinkSegmentTypePcuHourLane";

  private static final String SET_MAXIMUM_DENSITY_LINK_SEGMENT_TYPE = "setMaximumDensityLinkSegmentTypePcuKmLane";

  private static final String SET_FORCE_CONCAVE_FREE_FLOW_BRANCH = "setForceConcaveFreeFlowBranch";

  private static final String SET_CRITICAL_SPEED_FACTOR = "setCriticalSpeedFactor";

  /**
   * Constructor
   *
   */
  protected QuadraticLinearFundamentalDiagramConfigurator() {
    super(QuadraticLinearFundamentalDiagramComponent.class);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment. This only impacts the backward
   * wave speed used to keep the FD viable.
   *
   * @param linkSegment         the specified link segment
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentPcuHourLane(
          final MacroscopicLinkSegment linkSegment, final double capacityPcuHourLane) {
    registerDelayedMethodCall(SET_CAPACITY_LINK_SEGMENT, linkSegment, capacityPcuHourLane);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment. This only impacts
   * the backward wave speed used to keep the FD viable. one to change the capacity.
   *
   * @param linkSegment         the specified link segment
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentPcuKmLane(
          final MacroscopicLinkSegment linkSegment, final double maxDensityPcuKmLane) {
    registerDelayedMethodCall(SET_MAXIMUM_DENSITY_LINK_SEGMENT, linkSegment, maxDensityPcuKmLane);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment type. This only impacts the backward wave speed used to keep the FD viable.
   *
   * @param linkSegmentType     the specified link segment type
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentTypePcuHourLane(
          final MacroscopicLinkSegmentType linkSegmentType, final double capacityPcuHourLane) {
    registerDelayedMethodCall(SET_CAPACITY_LINK_SEGMENT_TYPE, linkSegmentType, capacityPcuHourLane);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment type.
   * This only impacts the backward wave speed used to keep the FD viable. one to change the capacity.
   *
   * @param linkSegmentType     the specified link segment type
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentTypePcuKmLane(
          final MacroscopicLinkSegmentType linkSegmentType, final double maxDensityPcuKmLane) {
    registerDelayedMethodCall(SET_MAXIMUM_DENSITY_LINK_SEGMENT_TYPE, linkSegmentType, maxDensityPcuKmLane);
  }

  /** Set factor to apply to the free speed to obtain critical speed in case forceConcaveFreeFlowBranch is active,
   * and the critical speed is not explicitly set to a lower value than the free speed
   *
   * @param criticalSpeedFactor to use
   */
  public void setCriticalSpeedFactor(double criticalSpeedFactor) {
    registerDelayedMethodCall(SET_CRITICAL_SPEED_FACTOR, criticalSpeedFactor);
  }

  /** Set flag to indicate of we force a concave free flow in case critical speed is defined or inferred to be
   * greater or equal than the free speed
   *
   * @param forceConcaveFreeFlowBranch flag to set
   */
  public void setForceConcaveFreeFlowBranch(boolean forceConcaveFreeFlowBranch) {
    registerDelayedMethodCall(SET_FORCE_CONCAVE_FREE_FLOW_BRANCH, forceConcaveFreeFlowBranch);
  }

}