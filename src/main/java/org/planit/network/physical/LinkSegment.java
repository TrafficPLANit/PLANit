package org.planit.network.physical;

import java.util.logging.Logger;

import org.planit.network.EdgeSegment;
import org.planit.utils.IdGenerator;

public class LinkSegment extends EdgeSegment {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(LinkSegment.class.getName());
        
	/**
	 * unique internal identifier 
	 */
	protected final long linkSegmentId;	

	/**
	 * segment's number of lanes
	 */
	protected int numberOfLanes = DEFAULT_NUMBER_OF_LANES;	
	
	/**
	 * Maximum link speed imposed irrespective of mode
	 */
	protected double maximumSpeed = DEFAULT_MAX_SPEED; 
	
/** 
 * Generate unique link segment id
 * 
 * @return       id of this link segment
 */
	protected static int generateLinkSegmentId() {
		return IdGenerator.generateId(LinkSegment.class);
	}		
	
/**
 * Constructor
 * 
 * @param parentLink           parent link of segment
 * @param directionAB         direction of travel
 */
	protected LinkSegment(Link parentLink, boolean directionAB){
		super(parentLink, directionAB);
		this.linkSegmentId = generateLinkSegmentId();
	}
		
	// Public 
	
	/**
	 *  Default number of lanes
	 */		
	public static final short DEFAULT_NUMBER_OF_LANES = 1;
	
	/**
	 * Default maximum speed on a link segment
	 */
	public static final double DEFAULT_MAX_SPEED = 130;
	
	// Public getters - setters
		
	public long getLinkSegmentId() {
		return linkSegmentId;
	}
	
	public int getNumberOfLanes() {
		return numberOfLanes;
	}
	public void setNumberOfLanes(int numberOfLanes) {
		this.numberOfLanes = numberOfLanes;
	}		
	
	public double getMaximumSpeed() {
		return maximumSpeed;
	}

	public void setMaximumSpeed(double maximumSpeed) {
		this.maximumSpeed = maximumSpeed;
	}
	
	public Link getParentLink() {
		return (Link)getParentEdge();
	}
		
}
