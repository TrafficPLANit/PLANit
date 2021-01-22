package org.planit.zoning;

import org.planit.graph.DirectedVertexImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Zone;

/**
 * Centroid implementation
 *
 * @author gman6028
 *
 */
public class CentroidImpl extends DirectedVertexImpl implements Centroid {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 1122451267627721268L;

  /**
   * the zone this centroid represents
   */
  private Zone parentZone;

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
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param parentZone the parent zone of this Centroid
   */
  protected CentroidImpl(final IdGroupingToken groupId, final Zone parentZone) {
    super(groupId);
    setParentzone(parentZone);
  }

  /**
   * Copy constructor
   * 
   * @param centroidImpl to copy
   */
  protected CentroidImpl(CentroidImpl centroidImpl) {
    super(centroidImpl);
    setParentzone(centroidImpl.getParentZone());
  }

  // Public

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
  public CentroidImpl clone() {
    return new CentroidImpl(this);
  }

}
