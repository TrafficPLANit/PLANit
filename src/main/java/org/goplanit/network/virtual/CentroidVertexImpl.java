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
   * position of centroid vertex. When not explicitly set it is collected from its parent. If parent centroid and its
   * own position differ, a warning is issued
   *
   * @return centroid position
   */
  @Override
  public Point getPosition(){
    if(position!=null && parent.hasPosition() && position.equals(parent.getPosition())){
      LOGGER.warning("Collecting position for centroid vertex, but its position and that of its parent " +
          "centroid differ, this shouldn't happen");
    }

    if(position!=null){
      return position;
    }
    return getParent().getPosition();
  }

  /**
   * Position cannot be altered on centroid vertex when centroid that it is related to has already a position defined.
   * Only when centroid's position has no meaning, i.e., it is not set, one can assign a unique position to each centroid
   * vertex related to this centroid.
   *
   * @param position to set
   */
  @Override
  public void setPosition(Point position){
    if(getParent().hasPosition()){
      LOGGER.warning("IGNORE: Not allowed to overwrite position of centroid vertex, when parent centroid position exists");
    }else{
      this.position = position;
    }
  }
}
