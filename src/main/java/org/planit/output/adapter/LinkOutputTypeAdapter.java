package org.planit.output.adapter;

import java.util.List;

import org.planit.network.physical.LinkSegment;
import org.planit.userclass.Mode;

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
}
