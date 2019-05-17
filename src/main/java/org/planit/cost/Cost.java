package org.planit.cost;

import org.planit.exceptions.PlanItException;
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
     * Calculate the cost of travel along an edge segment for a specified mode
     * 
     * @param mode							the specified mode of travel
     * @param edgeSegment              the specified edge segment (which can be physical or virtual) 
     * @return  									the cost of travel along the specified segment
     * @throws PlanItException			thrown if there is an error
     */
	public double calculateSegmentCost(Mode mode, T edgeSegment) throws PlanItException;
	
}