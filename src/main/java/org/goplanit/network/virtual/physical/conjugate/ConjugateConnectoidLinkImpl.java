package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.graph.ConjugateEdgeImpl;
import org.goplanit.network.layer.physical.LinkImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
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

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidLinkImpl.class.getCanonicalName());

  /**
   * adjacent originals represented by this conjugate
   */
  protected final Pair<? extends EdgeSegment, ? extends EdgeSegment> originalPair;

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateConnectoidLinkImpl(ConjugateConnectoidLinkImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.originalPair = other.originalPair.copy();
  }

  /**
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @param original1 to use
   * @param original2 to use
   */
  protected ConjugateConnectoidLinkImpl(
          final IdGroupingToken groupId,
          final ConjugateConnectoidNode nodeA,
          final ConjugateConnectoidNode nodeB,
          final ConnectoidSegment original1,
          final ConnectoidSegment original2) {
    super(groupId, nodeA, nodeB);
    this.originalPair = Pair.of(original1, original2);
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
   * Geometry on conjugate connectoid link is created on-the-fly by joining the two nodes on its extremes (direct line).
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
  public Pair<? extends EdgeSegment, ? extends EdgeSegment> getOriginalAdjacentSegments() {
    return this.originalPair;
  }

}
