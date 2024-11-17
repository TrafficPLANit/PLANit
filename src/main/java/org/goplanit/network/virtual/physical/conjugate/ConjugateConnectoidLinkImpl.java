package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.network.layer.physical.LinkImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLink;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegment;
import org.locationtech.jts.geom.LineString;

import java.util.logging.Logger;

/**
 * Conjugate (non-directional) link class connecting two conjugate nodes.
 * <p>
 * Since a conjugate link is in fact a turn it may be that the node is a connectoid edge and its conjugate is not a conjugate node but a conjugate connectoid node. Therefore we use
 * conjugate directed vertices rather than require a conjugate node as the base class
 *
 * @author markr
 *
 */
public class ConjugateConnectoidLinkImpl
    extends LinkImpl<ConjugateConnectoidNode, ConjugateConnectoidSegment> implements ConjugateConnectoidLink {

  // Protected

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidLinkImpl.class.getCanonicalName());

  /**
   * adjacent original directed edges represented by this conjugate
   */
  protected final Pair<? extends DirectedEdge, ? extends DirectedEdge> originalEdges;

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateConnectoidLinkImpl(ConjugateConnectoidLinkImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.originalEdges = other.originalEdges.copy();
  }

  /**
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @param originalEdge1 to use
   * @param originalEdge2 to use
   */
  protected ConjugateConnectoidLinkImpl(
          final IdGroupingToken groupId,
          final ConjugateConnectoidNode nodeA,
          final ConjugateConnectoidNode nodeB,
          final DirectedEdge originalEdge1,
          final DirectedEdge originalEdge2) {
    super(groupId, nodeA, nodeB);
    this.originalEdges = Pair.of(originalEdge1, originalEdge2);
  }

  /**
   * Length not supported on conjugate edge, collect from original underlying edges instead if required
   * 
   * @return negative infinity
   */
  @Override
  public double getLengthKm() {
    LOGGER.warning("Length of conjugate is combination of underlying original geometries/lengths, " +
        "collect those instead, negative infinity returned");
    return Double.NEGATIVE_INFINITY;
  }

  /**
   * Length not supported on conjugate edge, set on original underlying edges instead if required
   * 
   * @param lengthInKm to use
   */
  @Override
  public void setLengthKm(double lengthInKm) {
    LOGGER.warning("Length of conjugate is combination of underlying original geometries/lengths, " +
        "set those instead");
  }

  /**
   * Geometry not supported on conjugate edge, collect from original underlying edge segments instead if required
   * 
   * @return null
   */
  @Override
  public LineString getGeometry() {
    LOGGER.warning("Geometry of conjugate is combination of underlying original geometries, " +
        "collect those instead, null returned");
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
  public ConjugateConnectoidLinkImpl shallowClone() {
    return new ConjugateConnectoidLinkImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidLinkImpl deepClone() {
    return new ConjugateConnectoidLinkImpl(this, true);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<? extends DirectedEdge, ? extends DirectedEdge> getOriginalAdjacentEdges() {
    return this.originalEdges;
  }

}
