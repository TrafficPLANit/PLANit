package org.goplanit.network.virtual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodes;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.zoning.Centroid;

/**
 * Conjugate version (edge-to-vertex-dual) of regular virtual network
 * 
 * @author markr
 *
 */
public class ConjugateVirtualNetworkImpl implements ConjugateVirtualNetwork {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateVirtualNetworkImpl.class.getCanonicalName());

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
    for (var connectoidEdge : originalVirtualNetwork.getConnectoidEdges()) {

      // TODO -> for the nodes -> do use tokenId so it can be homogeneised across all conjugate components
      // due to additional creation of nodes we can't keep it in sync with the original edges anywayy -> so change throughout

      var conjugateDummyNode = getConjugateConnectoidNodes().getFactory().registerNew(null); // no original network equivalent
      var conjugateNode = getConjugateConnectoidNodes().getFactory().registerNew(connectoidEdge);

      /* create "fake" conjugate connectoid edge (where one of the two conjugate connectoid nodes has no original network equivalent but reflects a conjugate centroid) */
      // TODO

      // create conjugate connectoid edge between the two nodes to create connectoid turn segments where either the incoming or outgoing original edge segment is null
      // this ensures we can have a generic path search algorithm where we consistently use either incoming or outgoing original edge segment costs
      // TODO
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Centroid, Collection<ConjugateConnectoidNode>> createCentroidToConjugateNodeMapping() {
    var mapping = new HashMap<Centroid, Collection<ConjugateConnectoidNode>>();
    for (ConjugateConnectoidNode conjugateNode : getConjugateConnectoidNodes()) {
      var originalEdge = conjugateNode.getOriginalEdge();
      Centroid centroid = null;
      if (originalEdge.getVertexA() instanceof Centroid) {
        centroid = (Centroid) originalEdge.getVertexA();
      } else if (originalEdge.getVertexB() instanceof Centroid) {
        centroid = (Centroid) originalEdge.getVertexB();
      } else {
        LOGGER.severe(String.format("Conjugate node's (%s) original edge not connected to centroid, this shouldn't happen", conjugateNode.getXmlId()));
      }
      var mappedConjugateNodes = mapping.get(centroid);
      if (mappedConjugateNodes == null) {
        mappedConjugateNodes = new ArrayList<ConjugateConnectoidNode>(2);
        mapping.put(centroid, mappedConjugateNodes);
      }
      mappedConjugateNodes.add(conjugateNode);
    }
    return mapping;
  }

}
