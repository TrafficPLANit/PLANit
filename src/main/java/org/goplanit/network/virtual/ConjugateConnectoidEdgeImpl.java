package org.goplanit.network.virtual;

import org.goplanit.graph.directed.ConjugateDirectedEdgeImpl;
import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdge;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.locationtech.jts.geom.LineString;

import java.util.logging.Logger;

/**
 * Conjugate (non-directional) connectoid edge class connecting two conjugate nodes. This conjugate only partly exists in the original network to be able to comprise the initial turn entering the network (at origin) or the last turn leaving the network (at destination).
 * <p>
 * Since a conjugate edge is in fact a turn it may be that the node is a connectoid edge and its conjugate is not a conjugate node but a conjugate connectoid node. Therefore we use
 * conjugate directed vertices rather than require a conjugate node as the base class
 *
 * @author markr
 *
 */
public class ConjugateConnectoidEdgeImpl extends ConjugateDirectedEdgeImpl<ConjugateConnectoidNode, ConjugateConnectoidSegment> implements ConjugateConnectoidEdge {

  // Protected

  /** the logger */
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
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first vertex of the edge
   * @param nodeB    the second  vertex of the edge
   * @param  originalConnectoidEdge of the conjugate
   */
  protected ConjugateConnectoidEdgeImpl(final IdGroupingToken groupId, final ConjugateConnectoidNode nodeA, final ConjugateConnectoidNode nodeB, final ConnectoidEdge originalConnectoidEdge) {
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
