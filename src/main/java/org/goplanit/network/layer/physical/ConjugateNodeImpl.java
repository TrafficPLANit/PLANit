/**
 *
 */
package org.goplanit.network.layer.physical;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Conjugate node representation connected to one or more conjugate (entry and exit) conjugate links.
 *
 * @author markr
 *
 */
public class ConjugateNodeImpl extends DirectedVertexImpl<ConjugateLinkSegment> implements ConjugateNode {

  /** UID */
  private static final long serialVersionUID = 4196503072938986885L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateNodeImpl.class.getCanonicalName());

  /** original this conjugate represents */
  protected final Link original;

  /**
   * Special case where the id is based on the original link and does not rely on generating based on token when recreating managed ids this should override the default behaviour
   * of generating an id based on token
   */
  @Override
  protected long generateAndSetId(IdGroupingToken tokenId) {
    return original.getId();
  }

  // Public

  /**
   * Conjugate Node constructor. Relies on original link to sync id with
   * 
   * @param original original this conjugate represents
   */
  protected ConjugateNodeImpl(final Link original) {
    super(original.getId());
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

  // Protected

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
