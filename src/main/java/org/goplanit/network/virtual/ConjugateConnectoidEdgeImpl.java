package org.goplanit.network.virtual;

import java.util.logging.Logger;

import org.goplanit.graph.directed.ConjugateDirectedEdgeImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;

/**
 * Conjugate (non-directional) connectoid edge class connecting two conjugate nodes. This conjugate only partly exists in the original network to be able to comprise the initial
 * turn entering the network (at origin) or the last turn leaving the network (at destination).
 * <p>
 * Since a conjugate edge is in fact a turn it may be that the node is a connectoid edge and its conjugate is not a conjugate node but a conjugate connectoid node. Therefore we use
 * conjugate directed vertices rather than require a conjugate node as the base class
 *
 * @author markr
 *
 */
public class ConjugateConnectoidEdgeImpl
    extends ConjugateDirectedEdgeImpl<ConjugateConnectoidNode, ConjugateConnectoidSegment>
    implements ConjugateConnectoidEdge {

  // Protected

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidEdgeImpl.class.getCanonicalName());

  /**
   * unique internal identifier across connectoid edges
   */
  protected long conjugateConnectoidEdgeId;

  /**
   * Set the connectoidEdgeId
   *
   * @param conjugateConnectoidEdgeId to set
   */
  protected void setConjugateConnectoidEdgeId(long conjugateConnectoidEdgeId) {
    this.conjugateConnectoidEdgeId = conjugateConnectoidEdgeId;
  }

  /**
   * recreate the internal connectoid edge id and set it
   *
   * @param tokenId to use
   * @return updated id
   */
  protected long recreateConjugateConnectoidEdgeId(IdGroupingToken tokenId) {
    long newConnectoidEdgeId = ConnectoidEdge.generateConnectoidEdgeId(tokenId);
    setConjugateConnectoidEdgeId(newConnectoidEdgeId);
    return newConnectoidEdgeId;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateConnectoidEdgeImpl(ConjugateConnectoidEdgeImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setConjugateConnectoidEdgeId(other.getConnectoidEdgeId());
  }

  /**
   * Constructor
   *
   * @param groupId,               contiguous id generation within this group for instances of this class
   * @param nodeA                  the first vertex of the edge
   * @param nodeB                  the second vertex of the edge
   * @param originalConnectoidEdge of the conjugate
   */
  protected ConjugateConnectoidEdgeImpl(final IdGroupingToken groupId, final ConjugateConnectoidNode nodeA, final ConjugateConnectoidNode nodeB,
      final ConnectoidEdge originalConnectoidEdge) {
    super(groupId, nodeA, nodeB, originalConnectoidEdge, null);
    setConjugateConnectoidEdgeId(ConnectoidEdge.generateConnectoidEdgeId(groupId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getConnectoidEdgeId() {
    return conjugateConnectoidEdgeId;
  }

  /**
   * Recreate internal ids: id and connectoid edge id
   *
   * @return recreated id
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateConjugateConnectoidEdgeId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegment registerConnectoidSegment(/*Conjugate*/ConnectoidSegment connectoidSegment, boolean directionAB) {
    return (ConjugateConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgeImpl shallowClone() {
    return new ConjugateConnectoidEdgeImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgeImpl deepClone() {
    return new ConjugateConnectoidEdgeImpl(this, true);
  }

}
