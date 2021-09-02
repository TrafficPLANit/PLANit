package org.planit.supply.fundamentaldiagram;

import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Configurator for Newell Fundamental diagram implementation. We allow one to overwrite the the capacity and maximum densities used for each FD but not the free speed, since the
 * free speed is a physical value that is a given. Note that a change on the link segment level takes precedence over a change on the link segment type, i.e., the most specific
 * overwrite is used in the final fundamental diagram applied on the link segment.
 * <p>
 * In absence of a capacity the capacity is computed by the point of intersection of the free flow branch and congested branch, which for Newell is defined by only the free flow
 * speed, maximum density and intersection points with the x-axis (density=0), by explicitly setting the capacity or moving the maximum density point the FD will adjust its
 * backward wave speed to accommodate any change comapred to the default computed capacity/maximum density.
 * 
 * @author markr
 */
public class NewellFundamentalDiagramConfigurator extends FundamentalDiagramConfigurator<NewellFundamentalDiagramComponent> {

  private static final String SET_CAPACITY_LINK_SEGMENT = "setCapacityLinkSegmentPcuHourLane";

  private static final String SET_MAXIMUM_DENSITY_LINK_SEGMENT = "setMaximumDensityLinkSegmentPcuKmLane";

  private static final String SET_CAPACITY_LINK_SEGMENT_TYPE = "setCapacityLinkSegmentTypePcuHourLane";

  private static final String SET_MAXIMUM_DENSITY_LINK_SEGMENT_TYPE = "setMaximumDensityLinkSegmentTypePcuKmLane";

  /**
   * Constructor
   * 
   */
  protected NewellFundamentalDiagramConfigurator() {
    super(NewellFundamentalDiagramComponent.class);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment. This only impacts the backward wave speed used to keep the FD viable.
   *
   * @param linkSegment         the specified link segment
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentPcuHourLane(final MacroscopicLinkSegment linkSegment, final double capacityPcuHourLane) {
    registerDelayedMethodCall(SET_CAPACITY_LINK_SEGMENT, linkSegment, capacityPcuHourLane);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment. This only impacts the backward wave speed used to keep the FD viable. one to change
   * the capacity.
   *
   * @param linkSegment         the specified link segment
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentPcuKmLane(final MacroscopicLinkSegment linkSegment, final double maxDensityPcuKmLane) {
    registerDelayedMethodCall(SET_MAXIMUM_DENSITY_LINK_SEGMENT, linkSegment, maxDensityPcuKmLane);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment type. This only impacts the backward wave speed used to keep the FD viable.
   *
   * @param linkSegmentType     the specified link segment type
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentTypePcuHourLane(final MacroscopicLinkSegmentType linkSegmentType, final double capacityPcuHourLane) {
    registerDelayedMethodCall(SET_CAPACITY_LINK_SEGMENT_TYPE, linkSegmentType, capacityPcuHourLane);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment type. This only impacts the backward wave speed used to keep the FD viable. one to
   * change the capacity.
   *
   * @param linkSegmentType     the specified link segment type
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentTypePcuKmLane(final MacroscopicLinkSegmentType linkSegmentType, final double maxDensityPcuKmLane) {
    registerDelayedMethodCall(SET_MAXIMUM_DENSITY_LINK_SEGMENT_TYPE, linkSegmentType, maxDensityPcuKmLane);
  }

}