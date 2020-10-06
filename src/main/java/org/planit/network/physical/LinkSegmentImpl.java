package org.planit.network.physical;

import org.planit.graph.EdgeSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;

/**
 * Link segment object representing physical links in the network and storing their properties
 *
 * @author gman6028, markr
 *
 */
public class LinkSegmentImpl extends EdgeSegmentImpl implements LinkSegment {

  /** generated UID */
  private static final long serialVersionUID = -4893553215218232006L;

  /**
   * unique internal identifier
   */
  protected long linkSegmentId;

  /**
   * segment's number of lanes
   */
  protected int numberOfLanes = DEFAULT_NUMBER_OF_LANES;

  /**
   * physical maximum speed on the link segment in km/h
   */
  protected double maximumSpeed = DEFAULT_MAX_SPEED;

  /**
   * Generate unique link segment id
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return id of this link segment
   */
  protected static long generateLinkSegmentId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, LinkSegment.class);
  }

  /**
   * Set the link segment id
   * 
   * @param linkSegmentId to use
   */
  protected void setLinkSegmentId(long linkSegmentId) {
    this.linkSegmentId = linkSegmentId;
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parentLink  parent link of segment
   * @param directionAB direction of travel
   * @throws PlanItException throw when error
   */
  protected LinkSegmentImpl(final IdGroupingToken groupId, final Link parentLink, final boolean directionAB) throws PlanItException {
    super(groupId, parentLink, directionAB);
    setLinkSegmentId(generateLinkSegmentId(groupId));
  }

  // Public

  // Public getters - setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLinkSegmentId() {
    return linkSegmentId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfLanes() {
    return numberOfLanes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNumberOfLanes(final int numberOfLanes) {
    this.numberOfLanes = numberOfLanes;
  }

  /**
   * This is the maximum speed that is physically present and a driver can observe from the signs on the road
   * 
   * @param maximumSpeed physical speed limit
   */
  public void setPhysicalSpeedLimitKmH(double maximumSpeed) {
    this.maximumSpeed = maximumSpeed;
  }

  /**
   * This is the maximum speed that is physically present and a driver can observe from the signs on the road
   * 
   * @return maximumSpeed
   */
  public double getPhysicalSpeedLimitKmH() {
    return this.maximumSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link getParentLink() {
    return (Link) getParentEdge();
  }

}
