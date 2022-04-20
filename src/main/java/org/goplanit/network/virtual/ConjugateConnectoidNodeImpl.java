/**
 *
 */
package org.goplanit.network.virtual;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConnectoidEdge;

/**
 * Conjugate node representation connected to one or more conjugate (entry and exit) conjugate links.
 *
 * @author markr
 *
 */
public class ConjugateConnectoidNodeImpl extends DirectedVertexImpl<ConjugateEdgeSegment> implements ConjugateConnectoidNode {

  /** UID */
  private static final long serialVersionUID = -6715134872902634906L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidNodeImpl.class.getCanonicalName());

  /** original this conjugate represents */
  protected final ConnectoidEdge original;

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
   * Conjugate connectoid node constructor. Relies on original connectoid edge to sync id with
   * 
   * @param original original this conjugate represents
   */
  protected ConjugateConnectoidNodeImpl(final ConnectoidEdge original) {
    super(original.getId());
    this.original = original;
  }

  /**
   * Copy constructor, see also {@code VertexImpl}
   * 
   * @param nodeImpl to copy
   */
  protected ConjugateConnectoidNodeImpl(ConjugateConnectoidNodeImpl nodeImpl) {
    super(nodeImpl);
    this.original = nodeImpl.original;
  }

  // Protected

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<ConjugateDirectedEdge> getEdges() {
    return (Collection<ConjugateDirectedEdge>) super.getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNodeImpl clone() {
    return new ConjugateConnectoidNodeImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdge getOriginalEdge() {
    return original;
  }

}
