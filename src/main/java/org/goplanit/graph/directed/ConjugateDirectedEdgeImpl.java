package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.LineString;

/**
 * Conjugate Edge implementation class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction which may carry
 * additional information for each particular direction of the edge.
 *
 * @author markr
 *
 */
public class ConjugateDirectedEdgeImpl<V extends ConjugateDirectedVertex, ES extends ConjugateEdgeSegment> extends DirectedEdgeImpl<V, ES> implements ConjugateDirectedEdge {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateDirectedEdgeImpl.class.getCanonicalName());

  /**
   * adjacent original directed edges represented by this conjugate
   */
  protected final Pair<DirectedEdge, DirectedEdge> originalEdges;

  /**
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first conjugate vertex in the link
   * @param vertexB  second conjugate vertex in the link
   * @param originalEdge1 to use
   * @param originalEdge2 to use
   */
  protected ConjugateDirectedEdgeImpl(final IdGroupingToken groupId, final V vertexA, final V vertexB, final DirectedEdge originalEdge1,
      final DirectedEdge originalEdge2) {
    super(groupId, vertexA, vertexB);
    this.originalEdges = Pair.of(originalEdge1, originalEdge2);
  }

  /**
   * Copy Constructor. Edge segments are shallow copied and point to the passed in edge as their parent So additional effort is needed to make the new edge usable
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateDirectedEdgeImpl(ConjugateDirectedEdgeImpl<V,ES> other, boolean deepCopy) {
    super(other, deepCopy);
    this.originalEdges = other.originalEdges.copy(); // not owned so never deep copied
  }

  /**
   * Length not supported on conjugate edge, collect from original underlying edges instead if required
   * 
   * @return negative infinity
   */
  @Override
  public double getLengthKm() {
    LOGGER.warning("Length of conjugate is combination of underlying original geometries/lengths, collect those instead, negative infinity returned");
    return Double.NEGATIVE_INFINITY;
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
   * Geometry not supported on conjugate edge, collect from original underlying edge segments instead if required
   * 
   * @return null
   */
  @Override
  public LineString getGeometry() {
    LOGGER.warning("Geometry of conjugate is combination of underlying original geometries, collect those instead, null returned");
    return null;
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
  public Pair<DirectedEdge, DirectedEdge> getOriginalAdjacentEdges() {
    return originalEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdgeImpl<V, ES> clone() {
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
