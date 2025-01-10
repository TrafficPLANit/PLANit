/**
 *
 */
package org.goplanit.network.layer.physical;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.Link;
import org.locationtech.jts.geom.Point;

/**
 * Conjugate node representation connected to one or more conjugate (entry and exit) conjugate links.
 *
 * @author markr
 *
 */
public class ConjugateNodeImpl extends NodeImpl<ConjugateLinkSegment> implements ConjugateNode {


  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateNodeImpl.class.getCanonicalName());

  /** original this conjugate represents */
  protected final Link original;

  // Public

  /**
   * Conjugate Node constructor.
   *
   * @param idToken to use
   * @param original original this conjugate represents
   */
  protected ConjugateNodeImpl(IdGroupingToken idToken, final Link original) {
    super(idToken);
    this.original = original;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  protected ConjugateNodeImpl(ConjugateNodeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.original = other.original;
  }


  /**
   * conjugate derived position
   *
   * @return derive conjugate position
   */
  @Override
  public Point getPosition() {
    // explicitly use ConjugateVertex interface implementation otherwise it defaults to the extended directed vertex
    // which is not helpful here
    return ConjugateNode.super.getPosition();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(final Point position) {
    LOGGER.warning("Geometry of conjugate node is derived from  underlying original geometries, " +
            "unable to explicitly step position directly, ignored");
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<ConjugateLink> getEdges() {
    return (Collection<ConjugateLink>) super.getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodeImpl shallowClone() {
    return new ConjugateNodeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodeImpl deepClone() {
    return new ConjugateNodeImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdge getOriginalEdge() {
    return original;
  }

}
