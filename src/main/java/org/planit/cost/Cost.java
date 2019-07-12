package org.planit.cost;

import org.planit.network.EdgeSegment;
import org.planit.userclass.Mode;

/**
 * Cost of an EdgeSegment  
 * 
 * @author markr
 *
 */
public interface Cost<T extends EdgeSegment> {

    /**
     * Returns the cost of travel along an edge segment for a specified mode
     * 
     * @param mode							the specified mode of travel
     * @param edgeSegment              the specified edge segment (which can be physical or virtual) 
     * @return  									the cost of travel along the specified segment
     */
	public double getSegmentCost(Mode mode, T edgeSegment);
	
}