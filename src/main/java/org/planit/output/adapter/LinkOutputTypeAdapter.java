package org.planit.output.adapter;

import org.planit.output.property.OutputProperty;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;

/**
 * Interface defining the methods required for a link output adapter
 * 
 * @author gman6028
 *
 */
public interface LinkOutputTypeAdapter extends OutputTypeAdapter {

  /**
   * collect the infrastructure layer id this mode resides on
   * 
   * @param mode to collect layer id for
   * @return infrastructure layer id, null if not found
   */
  public Long getInfrastructureLayerIdForMode(Mode mode);

  /**
   * Returns true if there is a flow through the current specified link segment for the specified mode
   * 
   * @param linkSegment specified link segment
   * @param mode        specified mode
   * @return true is there is flow through this link segment, false if the flow is zero
   */
  public boolean isFlowPositive(LinkSegment linkSegment, Mode mode);

  /**
   * Return a Link segments for this assignment
   * 
   * @param infrastructureLayerId to collect link segments for
   * @return a List of link segments for this assignment
   */
  public LinkSegments<? extends LinkSegment> getPhysicalLinkSegments(long infrastructureLayerId);

  /**
   * Return the value of a specified output property of a link segment
   * 
   * @param outputProperty     the specified output property
   * @param linkSegment        the specified link segment
   * @param mode               the current mode
   * @param timePeriod         the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  public Object getLinkOutputPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier);
}
