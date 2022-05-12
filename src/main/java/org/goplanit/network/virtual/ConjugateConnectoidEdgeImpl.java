package org.goplanit.network.virtual;

import java.util.logging.Logger;

import org.goplanit.graph.directed.ConjugateDirectedEdgeImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdge;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidEdge;

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
public class ConjugateConnectoidEdgeImpl extends ConjugateDirectedEdgeImpl<ConjugateConnectoidNode, ConjugateConnectoidSegment> implements ConjugateConnectoidEdge {

  // Protected

  /**
   * Generated UID
   */
  private static final long serialVersionUID = -5253510472753048901L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateConnectoidEdgeImpl.class.getCanonicalName());

  /**
   * Copy constructor, geometry is deep copied, see also {@code LinkImpl} copy constructed
   *
   * @param conjugateLinkImpl to copy
   */
  protected ConjugateConnectoidEdgeImpl(ConjugateConnectoidEdgeImpl conjugateLinkImpl) {
    super(conjugateLinkImpl);
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgeImpl clone() {
    return new ConjugateConnectoidEdgeImpl(this);
  }

}
