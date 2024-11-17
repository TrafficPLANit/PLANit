package org.goplanit.network.layer.physical;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.Set;

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
   * Set the link segment id
   * 
   * @param linkSegmentId to use
   */
  protected void setLinkSegmentId(long linkSegmentId) {
    this.linkSegmentId = linkSegmentId;
  }

  /**
   * recreate the internal link segment id and set it
   * 
   * @param tokenId to use
   * @return updated id
   */
  protected long recreateLinkSegmentId(IdGroupingToken tokenId) {
    long newLinkSegmentId = LinkSegment.generateLinkSegmentId(tokenId);
    setLinkSegmentId(newLinkSegmentId);
    return newLinkSegmentId;
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   */
  protected LinkSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) {
    this(groupId, null, directionAB);
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parentLink  parent link of segment
   * @param directionAB direction of travel
   */
  protected LinkSegmentImpl(final IdGroupingToken groupId, final Link parentLink, final boolean directionAB) {
    super(groupId, parentLink, directionAB);
    setLinkSegmentId(LinkSegment.generateLinkSegmentId(groupId));
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected LinkSegmentImpl(LinkSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setLinkSegmentId(other.getLinkSegmentId());
    setNumberOfLanes(other.getNumberOfLanes());
    setPhysicalSpeedLimitKmH(other.getPhysicalSpeedLimitKmH());
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
  public boolean isModeAllowed(Mode mode) {
    throw new PlanItRunTimeException("isModeAllowed must be implemented by derived class, as LinkSegmentImpl does not " +
        "cater for modes via members");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAllowedModes() {
    throw new PlanItRunTimeException("getAllowedModes must be implemented by derived class, as LinkSegmentImpl " +
        "does not cater for modes via members");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLinkSegmentId() {
    return linkSegmentId;
  }

  // Public

  // Public getters - setters

  /**
   * {@inheritDoc}
   */
  @Override
  public Link getParent() {
    return (Link) super.getParent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getDownstreamVertex() {
    return (Node) super.getDownstreamVertex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getUpstreamVertex() {
    return (Node) super.getUpstreamVertex();
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
  public LinkSegment setNumberOfLanes(final int numberOfLanes) {
    this.numberOfLanes = numberOfLanes;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public LinkSegment setPhysicalSpeedLimitKmH(double maximumSpeed) {
    this.physicalSpeedLinkKmh = maximumSpeed;
    return this;
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
  public LinkSegmentImpl shallowClone(){
    return new LinkSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegmentImpl deepClone(){
    return new LinkSegmentImpl(this, true);
  }

}
