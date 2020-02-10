package org.planit.output.adapter;

import java.util.List;

import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * Interface defining the methods required for a link output adapter
 * 
 * @author gman6028
 *
 */
public interface LinkOutputTypeAdapter extends OutputTypeAdapter {

	/**
	 * Returns true if there is a flow through the current specified link segment for the specified mode
	 * 
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @return true is there is flow through this link segment, false if the flow is zero
	 */
	public boolean isFlowPositive(LinkSegment linkSegment, Mode mode);
	
    /**
     * Return a List of link segments for this assignment
     * 
     * @return a List of link segments for this assignment
     */
    public List<LinkSegment> getLinkSegments();

    /**
     * Return the value of a specified output property of a link segment
     * 
     * @param outputProperty the specified output property
     * @param linkSegment the specified link segment
     * @param mode the current mode
     * @param timePeriod the current time period
     * @param timeUnitMultiplier the multiplier for time units 
     * @return the value of the specified output property (or an Exception if an error occurs)
     */
	public Object getLinkOutputPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier);
}