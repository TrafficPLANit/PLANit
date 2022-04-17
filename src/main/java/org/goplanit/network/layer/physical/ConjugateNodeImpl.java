/**
 *
 */
package org.goplanit.network.layer.physical;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Conjugate node representation connected to one or more conjugate (entry and exit) conjugate links
 *
 * @author markr
 *
 */
public class ConjugateNodeImpl extends NodeImpl<ConjugateLinkSegment> implements ConjugateNode {

  /** UID */
  private static final long serialVersionUID = 4196503072938986885L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateNodeImpl.class.getCanonicalName());

  /** original this conjugate represents */
  protected final Link original;

  // Public

  /**
   * Node constructor
   * 
   * @param groupId  contiguous id generation within this group for instances of this class
   * @param original original this conjugate represents
   */
  protected ConjugateNodeImpl(final IdGroupingToken groupId, final Link original) {
    super(groupId);
    this.original = original;
  }

  /**
   * Copy constructor, see also {@code VertexImpl}
   * 
   * @param nodeImpl to copy
   */
  protected ConjugateNodeImpl(ConjugateNodeImpl nodeImpl) {
    super(nodeImpl);
    this.original = nodeImpl.original;
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
  public ConjugateNodeImpl clone() {
    return new ConjugateNodeImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdge getOriginalEdge() {
    return original;
  }

}
