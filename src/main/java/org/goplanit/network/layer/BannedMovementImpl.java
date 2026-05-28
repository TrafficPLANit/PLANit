package org.goplanit.network.layer;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.BannedMovement;

import java.util.logging.Logger;

/**
 * Movement represents a pair of link segments in a particular (single) direction.
 *
 * @author markr
 *
 */
public class BannedMovementImpl extends ExternalIdAbleImpl implements BannedMovement {

  /** serial UID */
  private static final long serialVersionUID = 1L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BannedMovementImpl.class.getCanonicalName());

  /**
   * Store the from edge segment
   */
  private EdgeSegment segmentFrom;

  /**
   * Store the to edge segment
   */
  private EdgeSegment segmentTo;

  // Protected


  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param fromSegment  from segment to use
   * @param toSegment to segment to use
   */
  protected BannedMovementImpl(
      final IdGroupingToken groupId, final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    super(IdGenerator.generateId(groupId, BANNED_MOVEMENT_ID_CLASS));
    this.segmentFrom = fromSegment;
    this.segmentTo = toSegment;
  }

  /**
   * Copy constructor
   *
   * @param movement to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected BannedMovementImpl(BannedMovementImpl movement, boolean deepCopy) {
    super(movement);
    this.segmentFrom = movement.segmentFrom;
    this.segmentTo = movement.segmentTo;
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getSegmentFrom() {
    return segmentFrom;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getSegmentTo() {
    return segmentTo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSegmentFrom(EdgeSegment segment) {
    this.segmentFrom = segment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSegmentTo(EdgeSegment segment) {
    this.segmentTo = segment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovementImpl shallowClone() {
    return new BannedMovementImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovementImpl deepClone() {
    return new BannedMovementImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setId(IdGenerator.generateId(tokenId, getIdClass()));
    return getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends IdAble> getIdClass() {
    return BANNED_MOVEMENT_ID_CLASS;
  }
}
