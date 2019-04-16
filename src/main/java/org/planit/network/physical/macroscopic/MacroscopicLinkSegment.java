package org.planit.network.physical.macroscopic;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.userclass.Mode;

/** 
 * Link segment for macroscopic transport networks.
 * @author markr
 */
public class MacroscopicLinkSegment extends LinkSegment{
	
	// Protected
		
	/**
	 * the link type of this link containing all macroscopic features by user class
	 */
	protected MacroscopicLinkSegmentType linkSegmentType = null;
		
	// Public
		
/** 
 * Constructor
 * 
 * @param parentLink      the parent link of this link segment
 * @param directionAB    direction of traverl
 */
	public MacroscopicLinkSegment(@Nonnull Link parentLink, boolean directionAB) {
		super(parentLink, directionAB);
	}
	
/** 
 * Compute the total capacity by multiplying the capacity per lane and number of lanes
 * 
 * @return              linkSegmentCapacity in pcu
 */
	public double computeCapacity() {
		return getLinkSegmentType().getCapacityPerLane()*getNumberOfLanes();
	}
	
/** 
 * Compute the free flow travel time by mode, i.e. when the link's maximum speed might be capped by the mode's maximum speed
 * 
 * @param mode                       mode of travel
 * @return                                 freeFlowTravelTime for this mode
 * @throws PlanItException      thrown if there is an error      
 */
	public double computeFreeFlowTravelTime(Mode mode) throws PlanItException {
		double linkLength = getParentLink().getLength() ;
		double maximumSpeed = getMaximumSpeed();
		MacroscopicLinkSegmentTypeModeProperties  properties = getLinkSegmentType().getModeProperties();
		double computedMaximumSpeed = maximumSpeed;
		if (properties != null) {		
			double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties().getProperties(mode).getMaxSpeed();
			if ((maximumSpeed == 0.0) && (segmentTypeMaximumSpeed == 0.0)) {
				throw new PlanItException("No maximum speed defined for network link from anode reference " + getParentEdge().getVertexA().getExternalId() + " to bnode " + getParentEdge().getVertexB().getExternalId() );
			}
			computedMaximumSpeed = Math.min(maximumSpeed, segmentTypeMaximumSpeed);			
		}
		return linkLength / computedMaximumSpeed;
	}	
		
	// getters - setters
	
	public void setLinkSegmentType(MacroscopicLinkSegmentType linkSegmentType) {
		this.linkSegmentType = linkSegmentType;
	}	
	
	public MacroscopicLinkSegmentType getLinkSegmentType() {
		return linkSegmentType;
	}

}
