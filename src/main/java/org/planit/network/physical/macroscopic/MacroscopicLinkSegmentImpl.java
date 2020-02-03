package org.planit.network.physical.macroscopic;

import javax.annotation.Nonnull;

import org.planit.logging.PlanItLogger;
import org.planit.network.physical.LinkSegmentImpl;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;
import org.planit.utils.network.virtual.Centroid;

/**
 * Link segment for macroscopic transport networks.
 *
 * @author markr
 */
public class MacroscopicLinkSegmentImpl extends LinkSegmentImpl implements MacroscopicLinkSegment {

	// Protected

	/**generated UID */
	private static final long serialVersionUID = 4574164258794764853L;
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
	public MacroscopicLinkSegmentImpl(@Nonnull final Link parentLink, final boolean directionAB) {
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
	@Override
	public double computeCapacity() {
		return getLinkSegmentType().getCapacityPerLane() * getNumberOfLanes();
	}

	/**
	 * Compute the free flow travel time by mode, i.e. when the link's maximum speed
	 * might be capped by the mode's maximum speed
	 *
	 * If the input data are invalid, this method logs the problem and returns a
	 * negative value.
	 *
	 * @param mode mode of travel
	 * @return freeFlowTravelTime for this mode
	 */
	@Override
	public double computeFreeFlowTravelTime(final Mode mode) {
		final double linkLength = getParentLink().getLength();
		final double maximumSpeed = getMaximumSpeed(mode);
		final MacroscopicLinkSegmentTypeModeProperties properties = getLinkSegmentType().getModeProperties();
		double computedMaximumSpeed = maximumSpeed;
		if ((properties != null) && (getLinkSegmentType().getModeProperties().getProperties(mode) != null)) {
			final double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties().getProperties(mode).getMaxSpeed();
			if ((maximumSpeed == 0.0) && (segmentTypeMaximumSpeed == 0.0)) {
				if (getParentEdge().getVertexA() instanceof Centroid) {
					final long startId = ((Centroid) getParentEdge().getVertexA()).getParentZone().getExternalId();
					final long endId = ((Node) getParentEdge().getVertexB()).getExternalId();
					PlanItLogger.severe("No maximum speed defined for the origin connectoid from zone " + startId
							+ " to node " + endId);
					return -1.0;
				} else if (getParentEdge().getVertexB() instanceof Centroid) {
					final long startId = ((Node) getParentEdge().getVertexA()).getExternalId();
					final long endId = ((Centroid) getParentEdge().getVertexB()).getParentZone().getExternalId();
					PlanItLogger.severe("No maximum speed defined for the destination connectoid from node " + startId
							+ " to zone " + endId);
					return -1.0;
				} else {
					final long startId = ((Node) getParentEdge().getVertexA()).getExternalId();
					final long endId = ((Node) getParentEdge().getVertexB()).getExternalId();
					PlanItLogger.severe("No maximum speed defined for network link from anode reference " + startId
							+ " to bnode " + endId);
					return -1.0;
				}
			}
			computedMaximumSpeed = Math.min(maximumSpeed, segmentTypeMaximumSpeed);
		}
		return linkLength / computedMaximumSpeed;
	}

	// getters - setters

	@Override
	public void setLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType) {
		this.linkSegmentType = linkSegmentType;
	}

	@Override
	public MacroscopicLinkSegmentType getLinkSegmentType() {
		return linkSegmentType;
	}

}