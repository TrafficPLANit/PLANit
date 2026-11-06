package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Base class for all fundamental diagram configurator implementations
 * 
 * @author markr
 *
 * @param <T> fundamental diagram type
 */
public class FundamentalDiagramConfigurator<T extends FundamentalDiagramComponent> extends Configurator<T> {

  private static final String SET_CAPACITY_LINK_SEGMENT = "setCapacityLinkSegmentPcuHourLane";

  private static final String SET_MAXIMUM_DENSITY_LINK_SEGMENT = "setMaximumDensityLinkSegmentPcuKmLane";

  private static final String SET_CAPACITY_LINK_SEGMENT_TYPE = "setCapacityLinkSegmentTypePcuHourLane";

  private static final String SET_MAXIMUM_DENSITY_LINK_SEGMENT_TYPE = "setMaximumDensityLinkSegmentTypePcuKmLane";

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public FundamentalDiagramConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment.
   *
   * @param linkSegment         the specified link segment
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentPcuHourLane(
          final MacroscopicLinkSegment linkSegment, final double capacityPcuHourLane) {
    registerDelayedMethodCall(SET_CAPACITY_LINK_SEGMENT, linkSegment, capacityPcuHourLane);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment.
   *
   * @param linkSegment         the specified link segment
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentPcuKmLane(
          final MacroscopicLinkSegment linkSegment, final double maxDensityPcuKmLane) {
    registerDelayedMethodCall(SET_MAXIMUM_DENSITY_LINK_SEGMENT, linkSegment, maxDensityPcuKmLane);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment type.
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
   *
   * @param linkSegmentType     the specified link segment type
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentTypePcuKmLane(
          final MacroscopicLinkSegmentType linkSegmentType, final double maxDensityPcuKmLane) {
    registerDelayedMethodCall(SET_MAXIMUM_DENSITY_LINK_SEGMENT_TYPE, linkSegmentType, maxDensityPcuKmLane);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends FundamentalDiagram
   * 
   * @param fundamentalDiagram the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(FundamentalDiagramComponent fundamentalDiagram){
    super.configure((T) fundamentalDiagram);
  }

}
