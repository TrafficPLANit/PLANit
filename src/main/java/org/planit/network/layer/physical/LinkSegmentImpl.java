package org.planit.network.layer.physical;

import org.planit.graph.directed.EdgeSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;

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
  protected double physicalSpeedLinkKmh = DEFAULT_MAX_SPEED;

  /**
   * Generate unique link segment id
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return id of this link segment
   */
  protected static long generateLinkSegmentId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, LINK_SEGMENT_ID_CLASS);
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
   * recreate the internal link id and set it
   * 
   * @return
   */
  protected long recreateLinkSegmentId(IdGroupingToken tokenId) {
    long newLinkId = generateLinkSegmentId(tokenId);
    setLinkSegmentId(newLinkId);
    return newLinkId;
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   * @throws PlanItException throw when error
   */
  protected LinkSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) throws PlanItException {
    this(groupId, null, directionAB);
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

  /**
   * Copy constructor
   * 
   * @param linkSegmentImpl to copy
   */
  protected LinkSegmentImpl(LinkSegmentImpl linkSegmentImpl) {
    super(linkSegmentImpl);
    setLinkSegmentId(linkSegmentImpl.getLinkSegmentId());
    setNumberOfLanes(linkSegmentImpl.getNumberOfLanes());
    setPhysicalSpeedLimitKmH(linkSegmentImpl.getPhysicalSpeedLimitKmH());
  }

  // Public

  // Public getters - setters

  /**
   * Recreate internal ids: id and link segment id
   * 
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateLinkSegmentId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

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
    this.physicalSpeedLinkKmh = maximumSpeed;
  }

  /**
   * This is the maximum speed that is physically present and a driver can observe from the signs on the road
   * 
   * @return maximumSpeed
   */
  public double getPhysicalSpeedLimitKmH() {
    return this.physicalSpeedLinkKmh;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link getParentEdge() {
    return (Link) super.getParentEdge();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link getParentLink() {
    return getParentEdge();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegmentImpl clone() {
    return new LinkSegmentImpl(this);
  }

}
