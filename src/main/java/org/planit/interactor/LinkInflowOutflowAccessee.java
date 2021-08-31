package org.planit.interactor;

import org.planit.utils.network.layer.physical.LinkSegment;

/**
 * Link inflow/outflow accessee. Implementing classes provide access to their current link segments' inflow and outflow rates
 * 
 * @author markr
 *
 */
public interface LinkInflowOutflowAccessee extends InteractorAccessee {

  /**
   * {@inheritDoc}
   */
  @Override
  default Class<LinkInflowOutflowAccessor> getCompatibleAccessor() {
    return LinkInflowOutflowAccessor.class;
  }

  /**
   * Get the total inflow rate of a link segment over all modes
   * 
   * @param linkSegment the specified link segment
   * @return the inflow rate of this link segment
   */
  public double getLinkSegmentInflowPcuHour(LinkSegment linkSegment);

  /**
   * Get the outflow rate of a link segment over all modes
   * 
   * @param linkSegment the specified link segment
   * @return the outflow rate of this link segment
   */
  public double getLinkSegmentOutflowPcuHour(LinkSegment linkSegment);

  /**
   * Get link segment inflow rates for all link segments
   * 
   * @return link segment inflows for all modes
   */
  public double[] getLinkSegmentInflowsPcuHour();

  /**
   * Get link segment inflow rates for all link segments
   * 
   * @return link segment inflows for all modes
   */
  public double[] getLinkSegmentOutflowsPcuHour();

}
