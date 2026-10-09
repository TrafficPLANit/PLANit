package org.goplanit.network.layer;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.locationtech.jts.geom.*;

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

  /**
   * Only build geometry if dirty
   */
  private Geometry lazyGeometry;

  /** track state of geometry */
  boolean geometryDirty = true;

  /**
   * Construct combined geometry for the banned movement
   *
   * @return created Geometry
   */
  private Geometry computeDerivedGeometry() {
    if (segmentFrom == null || segmentTo == null ||
        !segmentFrom.hasGeometry() || !segmentTo.hasGeometry()) {
      return null;
    }

    try {
      var lineA = (LineString) segmentFrom.getGeometry();
      var lineB = (LineString) segmentTo.getGeometry();

      Coordinate[] coordsA = lineA.getCoordinates();
      Coordinate[] coordsB = lineB.getCoordinates();

      org.locationtech.jts.geom.CoordinateList fullPath = new org.locationtech.jts.geom.CoordinateList();

      //Add Segment From
      if (segmentFrom.isParentGeometryInSegmentDirection(false)) {
        fullPath.add(coordsA, false);
      } else {
        for (int i = coordsA.length - 1; i >= 0; i--) {
          fullPath.add(coordsA[i], false);
        }
      }

      // Add Segment To (Skipping the first point to prevent duplicate junction vertex)
      if (segmentTo.isParentGeometryInSegmentDirection(false)) {
        fullPath.add(coordsB, false, 1, coordsB.length - 1);
      } else {
        for (int i = coordsB.length - 2; i >= 0; i--) {
          fullPath.add(coordsB[i], false);
        }
      }

      // 3. Construct the entire continuous link
      GeometryFactory factory = new GeometryFactory();
      return factory.createLineString(fullPath.toCoordinateArray());

    } catch (Exception e) {
      return null;
    }
  }

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
    this.lazyGeometry = null;
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
    this.lazyGeometry = deepCopy ? computeDerivedGeometry() : movement.lazyGeometry;
    this.geometryDirty = movement.geometryDirty;
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
    this.geometryDirty = true;
    this.segmentFrom = segment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSegmentTo(EdgeSegment segment) {
    this.geometryDirty = true;
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

  /**
   * Invalidate the lazily tracked geometry through segments. When we know it has changed we can invalidate
   * so that next call to getGeometry wil refresh it
   */
  public void invalidateGeometry() {
    this.geometryDirty = true;
  }

  /**
   * Obtain geometry through segments.
   * @return the geometry
   */
  @Override
  public Geometry getGeometry() {
    if(geometryDirty){
      lazyGeometry = computeDerivedGeometry();
    }
    return lazyGeometry;
  }
}
