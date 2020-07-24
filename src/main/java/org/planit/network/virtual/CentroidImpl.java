package org.planit.network.virtual;

import org.planit.graph.VertexImpl;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Zone;

/**
 * Centroid implementation
 *
 * @author gman6028
 *
 */
public class CentroidImpl extends VertexImpl implements Centroid {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 1122451267627721268L;
  /**
   * the zone this centroid represents
   */
  protected final Zone parentZone;

  // Public

  /**
   * Constructor
   *
   *@param parent for id generation
   *@param parentZone
   *          the parent zone of this Centroid
   */
  public CentroidImpl(Object parent, final Zone parentZone) {
    super(parent);
    this.parentZone = parentZone;
  }

  // Getters-Setters

  /**
   * Return the parent zone of this centroid
   *
   * @return parent zone of this centroid
   */
  @Override
  public Zone getParentZone() {
    return this.parentZone;
  }

}
