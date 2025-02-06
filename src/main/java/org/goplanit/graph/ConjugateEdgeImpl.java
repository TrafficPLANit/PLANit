package org.goplanit.graph;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Logger;

import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.ConjugateEdge;
import org.goplanit.utils.graph.ConjugateVertex;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.LineString;

/**
 * ConjugateEdge class connecting two conjugate vertices. Original pair of adjacent edges is also provided
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
  protected final Pair<? extends EdgeSegment, ? extends EdgeSegment> originals;

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @param original1 to use
   * @param original2 to use
   */
  protected ConjugateEdgeImpl(
          final IdGroupingToken groupId,
          final V vertexA,
          final V vertexB,
          final EdgeSegment original1,
          final EdgeSegment original2) {
    super(groupId, vertexA, vertexB);
    originals = Pair.of(original1, original2);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateEdgeImpl(ConjugateEdgeImpl<V> other, boolean deepCopy) {
    super(other, deepCopy);
    originals = other.originals.copy();
  }

  /**
   * Static implementation of to be overwritten method that due to inheritance structure otherwise would
   * require code duplication.
   *
   * @param conjugateEdge to extract geometry from
   * @return geometry
   */
  public static LineString getGeometry(ConjugateEdge conjugateEdge){
    if(!conjugateEdge.getVertexA().hasPosition() || !conjugateEdge.getVertexB().hasPosition()){
      return null;
    }
    return PlanitJtsUtils.createLineString(
            conjugateEdge.getVertexA().getPosition().getCoordinate(),
            conjugateEdge.getVertexB().getPosition().getCoordinate());
  }

  /**
   * Static implementation of to be overwritten method that due to inheritance structure otherwise would
   * require code duplication.
   *
   * @param conjugateEdge to extract length in km from
   * @return length in km
   */
  public static double getLengthKm(ConjugateEdge conjugateEdge){
    DoubleAdder lengthAdder = new DoubleAdder();
    conjugateEdge.getOriginalAdjacentSegments().<Edge>both(e -> lengthAdder.add(e!= null ? e.getLengthKm() : 0.0));
    return lengthAdder.doubleValue();
  }

  /**
   * Length is sum of length of its underlying two edges. Computed on-the-fly. If any edge is null, it is assumed
   * length may be set to 0km for that edge.
   *
   * @return on-the-fly length calculation
   */
  @Override
  public double getLengthKm() {
    return getLengthKm(this);
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
   * Geometry on conjugate edge is created on-the-fly by joining the two nodes on its extremes (direct line). This to
   * be able to overlay the conjugate network on top of the original network and show how it differs. The actual geometry
   * can be retrieved from the underlying original edges.
   *
   * @return on-the-fly vertex connecting linestring
   */
  @Override
  public LineString getGeometry() {
    return getGeometry(this);
  }

  /**
   * Geometry not supported on conjugate edge, collect from original underlying edge segments instead if required
   * 
   * @param geometry to use
   */
  @Override
  public void setGeometry(LineString geometry) {
    LOGGER.warning("Conjugate edge is combination of underlying original geometries, ignored setGeometry()");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeImpl<V> shallowClone() {
    return new ConjugateEdgeImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeImpl<V> deepClone() {
    return new ConjugateEdgeImpl<>(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<? extends EdgeSegment, ? extends EdgeSegment> getOriginalAdjacentSegments() {
    return this.originals;
  }

}
