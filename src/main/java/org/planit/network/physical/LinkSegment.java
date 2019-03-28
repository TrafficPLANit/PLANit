package org.planit.network.physical;

import org.planit.network.EdgeSegment;
import org.planit.utils.IdGenerator;

public class LinkSegment extends EdgeSegment {
	
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
	
	/** generate unique link segment id
	 * @return linkSegmentId
	 */
	protected static int generateLinkSegmentId() {
		return IdGenerator.generateId(LinkSegment.class);
	}		
	
	/**
	 * Constructor
	 * @param parentLink, parentLink of segment
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
