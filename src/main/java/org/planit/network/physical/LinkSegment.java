package org.planit.network.physical;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.network.EdgeSegment;
import org.planit.utils.IdGenerator;

/**
 * Link segment object representing physical links in the network and storing
 * their properties
 * 
 * @author gman6028
 *
 */
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
     * Map of maximum speeds along this link for each mode
     */
    protected Map<Long, Double> maximumSpeedMap;

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
    protected LinkSegment(Link parentLink, boolean directionAB) {
        super(parentLink, directionAB);
        maximumSpeedMap = new HashMap<Long, Double>();
        this.linkSegmentId = generateLinkSegmentId();
    }

    // Public

    /**
     * Default number of lanes
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

    /**
     * Return the maximum speed along this link for a specified mode
     * 
     * @param modeExternalId
     *            external id of the specified mode
     * @return maximum speed along this link for the specified mode
     */
    public double getMaximumSpeed(long modeExternalId) {
         return maximumSpeedMap.get(modeExternalId);
    }

    /**
     * Set the maximum speed along this link for a specified mode
     * 
     * @param modeExternalId
     *            external id of the specified mode
     * @param maximumSpeed
     *            maximum speed along this link for the specified mode
     */
    public void setMaximumSpeed(long modeExternalId, double maximumSpeed) {
        maximumSpeedMap.put(modeExternalId, maximumSpeed);
    }

    /**
     * Set the Map maximum speed for each mode
     * 
     * @param maximumSpeedMap Map of speed values for each mode
     */
    public void setMaximumSpeedMap(Map<Long, Double> maximumSpeedMap) {
        this.maximumSpeedMap = maximumSpeedMap;
    }

    /**
     * Return the parent link of this link segment
     * 
     * @return Link object which is the parent of this link segment
     */
    public Link getParentLink() {
        return (Link) getParentEdge();
    }

}
