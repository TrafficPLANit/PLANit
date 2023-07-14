package org.goplanit.zoning;

import org.goplanit.utils.id.*;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.Zone;
import org.locationtech.jts.geom.Point;

/**
 * Centroid implementation
 *
 * @author gman6028, markr
 *
 */
public class CentroidImpl extends IdAbleImpl implements Centroid {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 1122451267627721268L;

  /**
   * the zone this centroid represents
   */
  private Zone parentZone;

  /** name of the centroid */
  private String name;

  /** location of the centroid */
  private Point position;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, CENTROID_ID_CLASS);
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected CentroidImpl(final IdGroupingToken groupId) {
    this(groupId, null);
  }

  /**
   * Constructor
   *
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param parentZone The parent zone of this Centroid
   */
  protected CentroidImpl(final IdGroupingToken groupId, final Zone parentZone) {
    super(generateId(groupId));
    setParentZone(parentZone);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected CentroidImpl(final CentroidImpl other, boolean deepCopy) {
    super(other);
    setParentZone(other.getParentZone());
    setName(other.getName());
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidImpl shallowClone() {
    return new CentroidImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidImpl deepClone() {
    return new CentroidImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone getParentZone() {
    return this.parentZone;
  }

  /**
   * Set the parent zone
   *
   * @param parentZone to set
   */
  @Override
  public void setParentZone(Zone parentZone) {
    this.parentZone = parentZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point getPosition() {
    return position;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(Point position) {
    this.position = position;
  }

}
