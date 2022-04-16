package org.goplanit.graph;

import java.util.logging.Logger;

import org.goplanit.utils.graph.ConjugateEdge;
import org.goplanit.utils.graph.ConjugateVertex;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.LineString;

/**
 * ConjugateEdge class connecting two conjugate vertices. ORiginal pair of adjacent edges is also provided
 *
 * @author markr
 *
 */
public class ConjugateEdgeImpl<V extends ConjugateVertex> extends EdgeImpl<V> implements ConjugateEdge {

  /** UID */
  private static final long serialVersionUID = -8689706463623986248L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateEdgeImpl.class.getCanonicalName());

  /**
   * adjacent original edges represented by this conjugate
   */
  protected final Pair<Edge, Edge> originalEdges;

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   */
  protected ConjugateEdgeImpl(final IdGroupingToken groupId, final V vertexA, final V vertexB, final Edge originalEdge1, final Edge originalEdge2) {
    super(groupId, vertexA, vertexB);
    originalEdges = Pair.of(originalEdge1, originalEdge2);
  }

  /**
   * Copy constructor
   * 
   * @param conjugateEdgeImpl to copy
   */
  protected ConjugateEdgeImpl(ConjugateEdgeImpl<V> conjugateEdgeImpl) {
    super(conjugateEdgeImpl);
    originalEdges = conjugateEdgeImpl.originalEdges.copy();
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
  public ConjugateEdgeImpl<V> clone() {
    return new ConjugateEdgeImpl<V>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<Edge, Edge> getOriginalEdges() {
    return this.originalEdges;
  }

}
