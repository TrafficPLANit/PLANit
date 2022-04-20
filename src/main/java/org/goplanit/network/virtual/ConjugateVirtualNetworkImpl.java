package org.goplanit.network.virtual;

import java.util.HashMap;
import java.util.Map;

import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodes;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.VirtualNetwork;

/**
 * Conjugate version (edge-to-vertex-dual) of regular virtual network
 * 
 * @author markr
 *
 */
public class ConjugateVirtualNetworkImpl implements ConjugateVirtualNetwork {

  /**
   * Container for conjugate connectoid nodes
   */
  protected final ConjugateConnectoidNodesImpl conjugateConnectoidNodes;

  /** original virtual network this conjugate is based on */
  protected final VirtualNetwork originalVirtualNetwork;

  /**
   * Reset and re-populate entire conjugate virtual network based on current state of original virtual network this is the conjugate of
   */
  protected void update() {
    reset();

    /* connectoid edge -> conjugate connectoid node */
    Map<ConnectoidEdge, ConjugateConnectoidNode> originalEdgeToConjugateNode = new HashMap<>();
    for (var connectoidEdge : originalVirtualNetwork.getConnectoidEdges()) {
      var conjugateNode = getConjugateConnectoidNodes().getFactory().registerNew(connectoidEdge);
      originalEdgeToConjugateNode.put(connectoidEdge, conjugateNode);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @return
   */
  @Override
  public ConjugateConnectoidNodes getConjugateConnectoidNodes() {
    return conjugateConnectoidNodes;
  }

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation for instances of this class
   */
  public ConjugateVirtualNetworkImpl(final VirtualNetwork originalVirtualNetwork) {
    this.conjugateConnectoidNodes = new ConjugateConnectoidNodesImpl();
    this.originalVirtualNetwork = originalVirtualNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    conjugateConnectoidNodes.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    conjugateConnectoidNodes.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetwork getOriginalVirtualNetwork() {
    return originalVirtualNetwork;
  }

}
