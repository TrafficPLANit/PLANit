/**
 *
 */
package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.locationtech.jts.geom.Point;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Conjugate node representation connected to one or more conjugate (entry and exit) conjugate links.
 *
 * @author markr
 *
 */
public class ConjugateConnectoidNodeImpl
    extends DirectedVertexImpl<ConjugateEdgeSegment> implements ConjugateConnectoidNode {

  /** UID */
  private static final long serialVersionUID = -6715134872902634906L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidNodeImpl.class.getCanonicalName());

  /**
   * Unique (conjugate) node identifier
   */
  protected long conjugateNodeId;

  /** original this conjugate represents */
  protected final ConnectoidSegment original;

  /**
   * set the node id on this node
   *
   * @param nodeId to set
   */
  protected void setConjugateNodeId(long nodeId) {
    this.conjugateNodeId = nodeId;
  }

  /**
   * recreate the internal conjugate node id and set it
   *
   * @param tokenId to use
   * @return the created node id
   */
  protected long recreateConjugateNodeId(IdGroupingToken tokenId) {
    long newNodeId = generateNodeId(tokenId);
    setConjugateNodeId(newNodeId);
    return newNodeId;
  }

  // Public

  /**
   * Conjugate connectoid node constructor. Relies on original connectoid edge to sync id with
   * 
   * @param original original this conjugate represents
   * @param idToken to use
   */
  protected ConjugateConnectoidNodeImpl(final ConnectoidSegment original, final IdGroupingToken idToken) {
    super(idToken);
    this.original = original;
    this.conjugateNodeId = generateNodeId(idToken);
  }

  /**
   * Copy constructor, see also {@code VertexImpl}
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateConnectoidNodeImpl(ConjugateConnectoidNodeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.original = other.original;
    setConjugateNodeId(other.getNodeId());
    setName(other.getName());
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
    return ConjugateConnectoidNode.super.getPosition();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(final Point position) {
    LOGGER.warning("Geometry of conjugate connectoid node is derived from  underlying original geometries, " +
            "unable to explicitly step position directly, ignored");
  }

  @Override
  public long getNodeId() {
    return conjugateNodeId;
  }

  /**
   * Provide name of original edge segment parent (if any)
   * @return name
   */
  @Override
  public String getName() {
    return this.getOriginalEdgeSegment()!=null ? this.getOriginalEdgeSegment().getParentName() : null;
  }

  /**
   * Not allowed to be set, derived from original edge ignored
   *
   * @param name of the node
   */
  @Override
  public void setName(String name) {
    LOGGER.warning("name cannot be set on conjugate node, it is derived from original edge, ignored");
  }

  /**
   * Recreate id and node id
   *
   * @param tokenId to use
   * @return created id (updated link Id is not returned)
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateConjugateNodeId(tokenId);
    return super.recreateManagedIds(tokenId);
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
  public ConjugateConnectoidNodeImpl shallowClone() {
    return new ConjugateConnectoidNodeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNodeImpl deepClone() {
    return new ConjugateConnectoidNodeImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegment getOriginalEdgeSegment() {
    return original;
  }

}
