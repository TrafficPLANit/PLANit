package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.ConjugateEdgeImpl;
import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.LineString;

/**
 * Conjugate Edge implementation class connecting two vertices via some geometry. Each edge has one or
 * two underlying edge segments in a particular direction which may carry
 * additional information for each particular direction of the edge.
 *
 * @author markr
 * @param <V> type of vertex
 * @param <ES> type of segment
 */
public class ConjugateDirectedEdgeImpl<V extends ConjugateDirectedVertex, ES extends ConjugateEdgeSegment>
    extends DirectedEdgeImpl<V, ES> implements ConjugateDirectedEdge {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateDirectedEdgeImpl.class.getCanonicalName());

  /**
   * adjacent originals represented by this conjugate
   */
  protected final Pair<EdgeSegment, EdgeSegment> originals;

  /**
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first conjugate vertex in the link
   * @param vertexB  second conjugate vertex in the link
   * @param original1 to use
   * @param original2 to use
   */
  protected ConjugateDirectedEdgeImpl(
          final IdGroupingToken groupId,
          final V vertexA,
          final V vertexB,
          final EdgeSegment original1,
          final EdgeSegment original2) {
    super(groupId, vertexA, vertexB);
    this.originals = Pair.of(original1, original2);
  }

  /**
   * Copy Constructor. Edge segments are shallow copied and point to the passed in edge as their parent So additional effort is needed to make the new edge usable
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateDirectedEdgeImpl(ConjugateDirectedEdgeImpl<V,ES> other, boolean deepCopy) {
    super(other, deepCopy);
    this.originals = other.originals.copy(); // not owned so never deep copied
  }

  /**
   * Length not supported on conjugate edge, set on original underlying edges instead if required
   * 
   * @param lengthInKm to use
   */
  @Override
  public void setLengthKm(double lengthInKm) {
    LOGGER.warning("Length of conjugate is combination of underlying original geometries/lengths, set those instead");
  }

  /**
   * Length is sum of length of its underlying two edges. Computed on-the-fly. If any edge is null, it is assumed
   * length may be set to 0km for that edge.
   *
   * @return on-the-fly length calculation
   */
  @Override
  public double getLengthKm() {
    return ConjugateEdgeImpl.getLengthKm(this);
  }

  /**
   * Geometry on conjugate directed edge is created on-the-fly by joining the two nodes on its extremes (direct line).
   * This to be able to overlay the conjugate network on top of the original network and show how it differs.
   * The actual geometry can be retrieved from the underlying original edges. It is assumed the vertices have a coordinate.
   *
   * @return on-the-fly vertex connecting linestring
   */
  @Override
  public LineString getGeometry() {
    return ConjugateEdgeImpl.getGeometry(this);
  }

  /**
   * Geometry not supported on conjugate edge, collect from original underlying edge segments instead if required
   * 
   * @param geometry to use
   */
  @Override
  public void setGeometry(LineString geometry) {
    LOGGER.warning("Geometry of conjugate is combination of underlying original geometries, set those instead");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<EdgeSegment, EdgeSegment> getOriginalAdjacentSegments() {
    return originals;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdgeImpl<V, ES> shallowClone() {
    return new ConjugateDirectedEdgeImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdgeImpl<V, ES> deepClone() {
    return new ConjugateDirectedEdgeImpl<>(this, true);
  }

}
