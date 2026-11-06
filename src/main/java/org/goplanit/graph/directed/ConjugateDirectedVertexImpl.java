package org.goplanit.graph.directed;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.id.IdGroupingToken;
import org.locationtech.jts.geom.Point;

/**
 * Conjugate directed vertex representation connected to one or more entry and exit conjugate edges
 *
 * @author markr
 *
 */
public class ConjugateDirectedVertexImpl extends DirectedVertexImpl<ConjugateEdgeSegment>
        implements ConjugateDirectedVertex {

  /** UID */
  private static final long serialVersionUID = 3357507383489421626L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateDirectedVertexImpl.class.getCanonicalName());

  // Protected

  /** original this conjugate vertex represents */
  protected final EdgeSegment original;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param original original this conjugate represents in the conjugate graph
   */
  protected ConjugateDirectedVertexImpl(final IdGroupingToken groupId, final EdgeSegment original) {
    super(groupId);
    this.original = original;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateDirectedVertexImpl(ConjugateDirectedVertexImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.original = other.original;
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
    return ConjugateDirectedVertex.super.getPosition();
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
  public Iterable<ConjugateEdgeSegment> getEntryEdgeSegments() {
    return entryEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<ConjugateEdgeSegment> getExitEdgeSegments() {
    return exitEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends ConjugateDirectedEdge> getEdges() {
    return (Collection<? extends ConjugateDirectedEdge>) super.getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertexImpl shallowClone() {
    return new ConjugateDirectedVertexImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertexImpl deepClone() {
    return new ConjugateDirectedVertexImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getOriginalEdgeSegment() {
    return original;
  }

}
