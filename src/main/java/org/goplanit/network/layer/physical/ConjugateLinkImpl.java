package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
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
public class ConjugateLinkImpl extends DirectedEdgeImpl<ConjugateDirectedVertex, ConjugateLinkSegment> implements ConjugateLink {

  // Protected

  /** UID */
  private static final long serialVersionUID = 1017598997588544001L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateLinkImpl.class.getCanonicalName());

  /**
   * adjacent original directed edges represented by this conjugate
   */
  protected final Pair<Link, Link> originalLinks;

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateLinkImpl(ConjugateLinkImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.originalLinks = other.originalLinks.copy();
  }

  /**
   * Constructor
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @param originalLink1 to use
   * @param originalLink2 to use
   */
  protected ConjugateLinkImpl(final IdGroupingToken groupId, final ConjugateDirectedVertex nodeA, final ConjugateDirectedVertex nodeB, final Link originalLink1,
      final Link originalLink2) {
    super(groupId, nodeA, nodeB);
    this.originalLinks = Pair.of(originalLink1, originalLink2);
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
  public Pair<? extends Link, ? extends Link> getOriginalAdjacentEdges() {
    return this.originalLinks;
  }

}
