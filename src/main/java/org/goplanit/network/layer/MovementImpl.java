package org.goplanit.network.layer;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Movement;

import java.util.logging.Logger;

/**
 * Movement represents a pair of link segments in a particular (single) direction.
 *
 * @author markr
 *
 */
public class MovementImpl extends ExternalIdAbleImpl implements Movement {

  /** serial UID */
  private static final long serialVersionUID = 1L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MovementImpl.class.getCanonicalName());

  /** flag regarding access */
  private boolean banned;

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
   * @param banned flag
   */
  protected MovementImpl(
      final IdGroupingToken groupId, final EdgeSegment fromSegment, final EdgeSegment toSegment, boolean banned) {
    super(IdGenerator.generateId(groupId, MOVEMENT_ID_CLASS));
    this.segmentFrom = fromSegment;
    this.segmentTo = toSegment;
    this.banned = banned;
  }

  /**
   * Copy constructor
   *
   * @param movement to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MovementImpl(MovementImpl movement, boolean deepCopy) {
    super(movement);
    this.segmentFrom = movement.segmentFrom;
    this.segmentTo = movement.segmentTo;
    this.banned = movement.banned;
  }

  // Public

  @Override
  public boolean isPermissible() {
    return !banned;
  }

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
  public MovementImpl shallowClone() {
    return new MovementImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MovementImpl deepClone() {
    return new MovementImpl(this, true);
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
    return MOVEMENT_ID_CLASS;
  }
}
