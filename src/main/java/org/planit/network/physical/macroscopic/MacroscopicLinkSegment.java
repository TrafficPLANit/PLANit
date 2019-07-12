package org.planit.network.physical.macroscopic;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.network.virtual.Centroid;
import org.planit.network.physical.Node;
import org.planit.userclass.Mode;

/**
 * Link segment for macroscopic transport networks.
 * 
 * @author markr
 */
public class MacroscopicLinkSegment extends LinkSegment {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegment.class.getName());

	// Protected

	/**
	 * the link type of this link containing all macroscopic features by user class
	 */
	protected MacroscopicLinkSegmentType linkSegmentType = null;
	
	// Public

	/**
	 * Constructor
	 * 
	 * @param parentLink  the parent link of this link segment
	 * @param directionAB direction of travel
	 */
	public MacroscopicLinkSegment(@Nonnull Link parentLink, boolean directionAB) {
		super(parentLink, directionAB);
	}

	/**
	 * Return the total capacity for a specified mode
	 * 
	 * Compute the total capacity by multiplying the capacity per lane and number of
	 * lanes
	 * 
	 * @return linkSegmentCapacity in PCU
	 */
	public double computeCapacity() {
		return getLinkSegmentType().getCapacityPerLane() * getNumberOfLanes();
	}

	/**
	 * Compute the free flow travel time by mode, i.e. when the link's maximum speed
	 * might be capped by the mode's maximum speed
	 * 
	 * If the input data are invalid, this method logs the problem and returns a negative value.
	 * 
	 * @param mode mode of travel
	 * @return freeFlowTravelTime for this mode
	 */
	public double computeFreeFlowTravelTime(Mode mode) {
		double linkLength = getParentLink().getLength();
		double maximumSpeed = getMaximumSpeed(mode.getExternalId());
		MacroscopicLinkSegmentTypeModeProperties properties = getLinkSegmentType().getModeProperties();
		double computedMaximumSpeed = maximumSpeed;
		if ((properties != null) && (getLinkSegmentType().getModeProperties().getProperties(mode) != null)) {
			double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties().getProperties(mode).getMaxSpeed();
			if ((maximumSpeed == 0.0) && (segmentTypeMaximumSpeed == 0.0)) {
				if (getParentEdge().getVertexA() instanceof Centroid) {
					long startId = ((Centroid) getParentEdge().getVertexA()).getParentZone().getExternalId();
					long endId = ((Node) getParentEdge().getVertexB()).getExternalId();
					LOGGER.severe("No maximum speed defined for the origin connectoid from zone " + startId	+ " to node " + endId);
					return -1.0;
				} else if (getParentEdge().getVertexB() instanceof Centroid) {
					long startId = ((Node) getParentEdge().getVertexA()).getExternalId();
					long endId = ((Centroid) getParentEdge().getVertexB()).getParentZone().getExternalId();
					LOGGER.severe("No maximum speed defined for the destination connectoid from node " + startId + " to zone " + endId);
					return -1.0;
				} else {
					long startId = ((Node) getParentEdge().getVertexA()).getExternalId();
					long endId = ((Node) getParentEdge().getVertexB()).getExternalId();
					LOGGER.severe("No maximum speed defined for network link from anode reference " + startId + " to bnode " + endId);
					return -1.0;
				}
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

	public long getExternalId() {
		return externalId;
	}

}
