package org.goplanit.zoning;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.Zone;

/**
 * Centroid implementation
 *
 * @author gman6028, markr
 *
 */
public class CentroidImpl extends DirectedVertexImpl<EdgeSegment> implements Centroid {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 1122451267627721268L;

  /**
   * the zone this centroid represents
   */
  private Zone parentZone;

  /** name of the centroid */
  private String name;

  /**
   * Set the parent zone
   * 
   * @param parentZone to set
   */
  protected void setParentzone(Zone parentZone) {
    this.parentZone = parentZone;
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
    super(groupId);
    setParentzone(parentZone);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected CentroidImpl(final CentroidImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setParentzone(other.getParentZone());
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

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone getParentZone() {
    return this.parentZone;
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

}
