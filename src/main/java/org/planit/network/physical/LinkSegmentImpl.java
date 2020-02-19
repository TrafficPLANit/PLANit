package org.planit.network.physical;

import java.util.HashMap;
import java.util.Map;

import org.planit.graph.EdgeSegmentImpl;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * Link segment object representing physical links in the network and storing
 * their properties
 *
 * @author gman6028
 *
 */
public abstract class LinkSegmentImpl extends EdgeSegmentImpl implements LinkSegment {

    /**generated UID */
	private static final long serialVersionUID = -4893553215218232006L;

	/**
     * unique internal identifier
     */
    protected final long linkSegmentId;

    /**
     * segment's number of lanes
     */
    protected int numberOfLanes = DEFAULT_NUMBER_OF_LANES;

    /**
     * Map of maximum speeds along this link for each mode
     */
    protected Map<Mode, Double> maximumSpeedMap;
    
   /**
     * Generate unique link segment id
     *
     * @return id of this link segment
     */
    protected static int generateLinkSegmentId() {
        return IdGenerator.generateId(LinkSegment.class);
    }

    /**
     * Constructor
     *
     * @param parentLink
     *            parent link of segment
     * @param directionAB
     *            direction of travel
     */
    protected LinkSegmentImpl(final Link parentLink, final boolean directionAB) {
        super(parentLink, directionAB);
        maximumSpeedMap = new HashMap<Mode, Double>();
        this.linkSegmentId = generateLinkSegmentId();
    }

    // Public

    // Public getters - setters

    @Override
	public long getLinkSegmentId() {
        return linkSegmentId;
    }

    @Override
	public int getNumberOfLanes() {
        return numberOfLanes;
    }

    @Override
	public void setNumberOfLanes(final int numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
    }

    /**
     * Return the maximum speed along this link for a specified mode
     *
     * @param mode the specified mode
     * @return maximum speed along this link for the specified mode
     */
    @Override
	public double getMaximumSpeed(final Mode mode) {
    	return maximumSpeedMap.get(mode);
    }

    /**
     * Set the maximum speed along this link for a specified mode
     *
     * @param mode the specified mode
     * @param maximumSpeed
     *            maximum speed along this link for the specified mode
     */
    @Override
	public void setMaximumSpeed(final Mode mode, final double maximumSpeed) {
    	maximumSpeedMap.put(mode,  maximumSpeed);
    }

    /**
     * Set the Map maximum speed for each mode
     *
     * @param maximumSpeedMap Map of speed values for each mode
     */
    public void setMaximumSpeedMap(final Map<Mode, Double> maximumSpeedMap) {
    	this.maximumSpeedMap = maximumSpeedMap;
    }

    /**
     * Return the parent link of this link segment
     *
     * @return Link object which is the parent of this link segment
     */
    @Override
	public Link getParentLink() {
        return (Link) getParentEdge();
    }

}