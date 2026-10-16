package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.ConjugateEdgeImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.Link;
import org.locationtech.jts.geom.LineString;

/**
 * Conjugate (non-directional) link class connecting two conjugate nodes.
 * <p>
 * Since a conjugate link is in fact a turn it may be that the node is a connectoid edge and its conjugate is not a conjugate node but a conjugate connectoid node. Therefore we use
 * conjugate directed vertices rather than require a conjugate node as the base class
 *
 * @author markr
 *
 */
public class ConjugateLinkImpl
        extends LinkImpl<ConjugateDirectedVertex, ConjugateLinkSegment> implements ConjugateLink {

  // Protected

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateLinkImpl.class.getCanonicalName());

  /**
   * adjacent originals represented by this conjugate
   */
  protected final Pair<? extends EdgeSegment, ? extends EdgeSegment> originals;

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateLinkImpl(ConjugateLinkImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.originals = other.originals.copy();
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
  protected ConjugateLinkImpl(
          final IdGroupingToken groupId,
          final ConjugateDirectedVertex nodeA,
          final ConjugateDirectedVertex nodeB,
          final EdgeSegment original1,
          final EdgeSegment original2) {
    super(groupId, nodeA, nodeB);
    this.originals = Pair.of(original1, original2);
  }

  /**
   * Length is sum of length of its underlying two edges. Computed on-the-fly. If any link is null, it is assumed
   * length may be set to 0km for that link.
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
   * Geometry not supported on conjugate edge, collect from original underlying edge segments instead if required
   * 
   * @return null
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
  public ConjugateLinkImpl shallowClone() {
    return new ConjugateLinkImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkImpl deepClone() {
    return new ConjugateLinkImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<? extends EdgeSegment, ? extends EdgeSegment> getOriginalAdjacentSegments() {
    return this.originals;
  }

}
