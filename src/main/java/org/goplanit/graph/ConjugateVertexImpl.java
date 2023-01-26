package org.goplanit.graph;

import java.util.logging.Logger;

import org.goplanit.utils.graph.ConjugateEdge;
import org.goplanit.utils.graph.ConjugateVertex;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.id.IdGroupingToken;
import org.locationtech.jts.geom.Point;

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
   * Position cannot be obtained from conjugate vertex
   */
  @Override
  public Point getPosition() {
    LOGGER.warning("Position of conjugate is non-eistent depends on underlying edge geometry, collect those instead, null returned");
    return null;
  }

  /**
   * position cannot be set on conjugate vertex
   */
  @Override
  public void setPosition(final Point position) {
    LOGGER.warning("Position of conjugate is non-eistent depends on underlying edge geometry, set those instead");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVertexImpl clone() {
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
