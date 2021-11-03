package org.goplanit.interactor;

import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Link inflow/outflow accessee. Implementing classes provide access to their current link segments' inflow and outflow rates. In addition, it also requires access to the used
 * fundamental diagram
 * 
 * @author markr
 *
 */
public interface LinkInflowOutflowAccessee extends TrafficAssignmentComponentAccessee {

  /**
   * {@inheritDoc}
   */
  @Override
  public default Class<LinkInflowOutflowAccessor> getCompatibleAccessor() {
    return LinkInflowOutflowAccessor.class;
  }

  /**
   * Get the total inflow rate of a link segment over all modes
   * 
   * @param linkSegment the specified link segment
   * @return the inflow rate of this link segment
   */
  public default double getLinkSegmentInflowPcuHour(LinkSegment linkSegment) {
    return getLinkSegmentInflowsPcuHour()[(int) linkSegment.getId()];
  }

  /**
   * Get the outflow rate of a link segment over all modes
   * 
   * @param linkSegment the specified link segment
   * @return the outflow rate of this link segment
   */
  public default double getLinkSegmentOutflowPcuHour(LinkSegment linkSegment) {
    return getLinkSegmentOutflowsPcuHour()[(int) linkSegment.getId()];
  }

  /**
   * Get link segment inflow rates for all link segments, where index is based on id of the link segment.
   * 
   * @return link segment inflows for all modes
   */
  public abstract double[] getLinkSegmentInflowsPcuHour();

  /**
   * Get link segment outflow rates for all link segments, where index is based on id of the link segment.
   * 
   * @return link segment inflows for all modes
   */
  public abstract double[] getLinkSegmentOutflowsPcuHour();

}
