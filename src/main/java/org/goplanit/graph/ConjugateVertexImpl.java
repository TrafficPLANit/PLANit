package org.goplanit.graph;

import org.goplanit.utils.graph.ConjugateEdge;
import org.goplanit.utils.graph.ConjugateVertex;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.id.IdGroupingToken;
import org.locationtech.jts.geom.Point;

import java.util.logging.Logger;

/**
 * Conjugate vertex representation connected to one or more entry and exit conjugate edges
 *
 * @author markr
 *
 */
public class ConjugateVertexImpl extends VertexImpl<ConjugateEdge> implements ConjugateVertex {

  /** UID */
  private static final long serialVersionUID = -2481992417162214639L;

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateVertexImpl.class.getCanonicalName());

  /** original edge this conjugate represents */
  protected final Edge originalEdge;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param originalEdge representing the conjugate vertex
   */
  protected ConjugateVertexImpl(final IdGroupingToken groupId, final Edge originalEdge) {
    super(groupId, CONJUGATE_VERTEX_ID_CLASS);
    this.originalEdge = originalEdge;
  }

  /**
   * Copy constructor
   * 
   * @param conjugateVertexImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateVertexImpl(ConjugateVertexImpl conjugateVertexImpl, boolean deepCopy) {
    super(conjugateVertexImpl, deepCopy);
    this.originalEdge = conjugateVertexImpl.originalEdge; // not owned
  }

  // Public

  /**
   * conjugate derived position
   *
   * @return derive conjugate position
   */
  @Override
  public Point getPosition() {
    // explicitly use ConjugateVertex interface implementation otherwise it defaults to the extended directed vertex
    // which is not helpful here
    return ConjugateVertex.super.getPosition();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(final Point position) {
    LOGGER.warning("Geometry of conjugate directed vertex is derived from  underlying original geometries, " +
            "unable to explicitly step position directly, ignored");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVertexImpl shallowClone() {
    return new ConjugateVertexImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVertexImpl deepClone() {
    return new ConjugateVertexImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge getOriginalEdge() {
    return this.originalEdge;
  }

}
