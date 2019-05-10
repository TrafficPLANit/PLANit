package org.planit.network.physical.macroscopic;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.Vertex;
import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.network.virtual.Centroid;
import org.planit.network.physical.Node;
import org.planit.userclass.Mode;

/** 
 * Link segment for macroscopic transport networks.
 * @author markr
 */
public class MacroscopicLinkSegment extends LinkSegment{
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegment.class.getName());
        
	// Protected
		
	/**
	 * the link type of this link containing all macroscopic features by user class
	 */
	protected MacroscopicLinkSegmentType linkSegmentType = null;
	
	/** find out what the xternal id of the vertex is depending on its concrete class (centroid or node)
	 * @param vertex
	 * @return externalId
	 */
	protected long getExternalId(Vertex vertex) {
	    if (vertex instanceof Centroid) {
	        // if a centroid, we do not have an external id, the centroid id is both internal/external
            return ((Centroid) vertex).getParentZone().getId() + 1; //TODO: NO plus one, the centroid and zone ids do not start at zero!
        } else {
            return ((Node) vertex).getExternalId();
        }
	}
		
	// Public
		
    /** 
     * Constructor
     * 
     * @param parentLink      the parent link of this link segment
     * @param directionAB    direction of travel
     */
	public MacroscopicLinkSegment(@Nonnull Link parentLink, boolean directionAB) {
		super(parentLink, directionAB);
	}
	
    /** 
     * Return the total capacity
     * 
     * Compute the total capacity by multiplying the capacity per lane and number of lanes
     * 
     * @return linkSegmentCapacity in PCU
     */
     public double computeTotalLinkSegmentCapacity() {
         return getLinkSegmentType().getCapacityPerLane()*getNumberOfLanes();
     }
   
     /** When a mode is not present on the type it is not allowed on the link segment
     * @param mode
     * @return true when allowed, false otherwise
     */
    public boolean isModeAllowed(Mode mode) {
         return getLinkSegmentType().hasModeProperties(mode);
     }   
	
    /** 
     * Compute the free flow travel time by mode, i.e. when the link's maximum speed might be capped by the mode's maximum speed
     * 
     * @param mode                       mode of travel
     * @return                                 freeFlowTravelTime for this mode
     * @throws PlanItException      thrown if there is an error      
     */
	public double computeFreeFlowTravelTime(Mode mode) throws PlanItException {
	    // NEW
	    double freeFlowTravelTime = Double.POSITIVE_INFINITY;
	    if(isModeAllowed(mode))
	    {
            double linkLength = getParentLink().getLength() ;
            double maximumSpeed = getMaximumSpeed(mode.getId());
            MacroscopicLinkSegmentTypeModeProperties  properties = getLinkSegmentType().getModeProperties();    
            double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties().getProperties(mode).getMaxSpeed();
            double appliedSpeed = Math.min(maximumSpeed, segmentTypeMaximumSpeed);
            if ( appliedSpeed <= 0) {
                long vertexAId = getExternalId(getParentEdge().getVertexA());
                long vertexBId = getExternalId(getParentEdge().getVertexB());                
                throw new PlanItException("No maximum speed defined for network link from anode reference " + vertexAId + " to bnode " + vertexBId);
            }
            freeFlowTravelTime =  linkLength / appliedSpeed;          
	    }
	    return freeFlowTravelTime;  	      
	    
	    //OLD
//		double linkLength = getParentLink().getLength() ;
//		double maximumSpeed = getMaximumSpeed(mode.getId());
//		MacroscopicLinkSegmentTypeModeProperties  properties = getLinkSegmentType().getModeProperties();
//		double computedMaximumSpeed = maximumSpeed;
//		if (properties != null) {		
//			double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties().getProperties(mode).getMaxSpeed();
//			if ((maximumSpeed == 0.0) && (segmentTypeMaximumSpeed == 0.0)) {
//			    long startId;
//			    long endId;
//			    if (getParentEdge().getVertexA() instanceof Centroid) {
//			        startId = ((Centroid) getParentEdge().getVertexA()).getCentroidId() + 1; //TODO: NO plus one, the centroid and zone ids do not start at zero!
//			        endId = ((Node) getParentEdge().getVertexB()).getExternalId();
//			    } else if (getParentEdge().getVertexB() instanceof Centroid) {
//                    startId = ((Node) getParentEdge().getVertexA()).getExternalId();
//                    endId = ((Centroid) getParentEdge().getVertexB()).getCentroidId() + 1;
//			    } else {
//                    startId = ((Node) getParentEdge().getVertexA()).getExternalId();
//                    endId = ((Node) getParentEdge().getVertexB()).getExternalId();
//			    }
//			    throw new PlanItException("No maximum speed defined for network link from anode reference " + startId + " to bnode " + endId);
//			}
//			computedMaximumSpeed = Math.min(maximumSpeed, segmentTypeMaximumSpeed);			
//		}
//		return linkLength / computedMaximumSpeed;
	}	
		
	// getters - setters
	
    public void setLinkSegmentType(MacroscopicLinkSegmentType linkSegmentType) {
		this.linkSegmentType = linkSegmentType;
	}	
	
	public MacroscopicLinkSegmentType getLinkSegmentType() {
		return linkSegmentType;
	}

}
