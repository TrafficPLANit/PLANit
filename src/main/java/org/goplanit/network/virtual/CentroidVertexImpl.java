package org.goplanit.network.virtual;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.zoning.Centroid;
import org.locationtech.jts.geom.Point;

/**
 * The vertex with a direct relation to a centroid (for a given layer)
 */
public class CentroidVertexImpl extends DirectedVertexImpl<ConnectoidSegment> implements CentroidVertex {

  /** parent centroid of this vertex */
  private Centroid parent;

  /**
   * Constructor
   *
   * @param groupId for id generation
   * @param parentCentroid parent centroid this vertex represents on its layer
   */
  protected CentroidVertexImpl(final IdGroupingToken groupId, final Centroid parentCentroid) {
    super(groupId);
    this.parent = parentCentroid;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true perform deep copy, when false do not
   */
  protected CentroidVertexImpl(DirectedVertexImpl<ConnectoidSegment> other, boolean deepCopy) {
    super(other, deepCopy);
  }

  // GETTERS-SETTERS

  /**
   * {@inheritDoc}
   */
  @Override
  public Centroid getParent() {
    return parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setParent(Centroid parent) {
    this.parent = parent;
  }

  /**
   * position of centroid vertex is collected from its parent
   * @return centroid position
   */
  @Override
  public Point getPosition(){
    return getParent().getPosition();
  }

  /**
   * position cannot be altered on centrod vertex as it is derived from its parent. throws PlanitRunTimeException.
   */
  @Override
  public void setPosition(Point position){
    throw new PlanItRunTimeException("Not allowed to set position of centroid vertex, to be done via its parent centroid");
  }
}
