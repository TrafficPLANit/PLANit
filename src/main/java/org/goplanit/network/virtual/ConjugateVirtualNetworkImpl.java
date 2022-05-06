package org.goplanit.network.virtual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.virtual.*;
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

  /**
   * Container for conjugate connectoid edges
   */
  protected final ConjugateConnectoidEdgesImpl conjugateConnectoidEdges;

  /**
   * Container for conjugate connectoid edge segments
   */
  protected final ConjugateConnectoidSegmentsImpl conjugateConnectoidSegments;

  /** original virtual network this conjugate is based on */
  protected final VirtualNetwork originalVirtualNetwork;

  /**
   * Reset and re-populate entire conjugate virtual network based on current state of original virtual network this is the conjugate of
   */
  protected void update() {
    reset();
    
    Map<DirectedVertex, ConjugateConnectoidNode> dummyConjugatePerZone = new HashMap<>();

    /* connectoid edge -> conjugate connectoid node */
    for (var connectoidEdge : originalVirtualNetwork.getConnectoidEdges()) {

      var centroid = connectoidEdge.getCentroidVertex();
      var conjugateDummyNode = dummyConjugatePerZone.get(centroid);
      if(conjugateDummyNode == null) {
        conjugateDummyNode = getConjugateConnectoidNodes().getFactory().registerNew(null);
        dummyConjugatePerZone.put(centroid, conjugateDummyNode);
      }
      var conjugateNode = getConjugateConnectoidNodes().getFactory().registerNew(connectoidEdge);

      /* create "fake" conjugate connectoid edge (where one of the two conjugate connectoid nodes has no original network equivalent but reflects a conjugate centroid) */
      var conjugateEdge = getConjugateConnectoidEdges().getFactory().registerNew(conjugateDummyNode, conjugateNode, true, connectoidEdge);

      // create conjugate connectoid segments between the two nodes to create connectoid turn segments where either the incoming or outgoing original edge segment is null
      // this ensures we can have a generic path search algorithm where we consistently use either incoming or outgoing original edge segment costs
      getConjugateConnectoidEdgeSegments().getFactory().registerNew(conjugateEdge,true /*ab direction*/, true);
      getConjugateConnectoidEdgeSegments().getFactory().registerNew(conjugateEdge,false /*ba direction*/, true);
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

  @Override
  public ConjugateConnectoidEdges getConjugateConnectoidEdges() {
    return conjugateConnectoidEdges;
  }

  @Override
  public ConjugateConnectoidSegments getConjugateConnectoidEdgeSegments() {
    return conjugateConnectoidSegments;
  }

  /**
   * Constructor
   * 
   * @param idToken contiguous id generation for instances of this class
   */
  public ConjugateVirtualNetworkImpl(IdGroupingToken idToken, final VirtualNetwork originalVirtualNetwork) {
    this.conjugateConnectoidNodes = new ConjugateConnectoidNodesImpl(idToken);
    this.conjugateConnectoidEdges = new ConjugateConnectoidEdgesImpl(idToken);
    this.conjugateConnectoidSegments = new ConjugateConnectoidSegmentsImpl(idToken);
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
  public Map<Centroid, ConjugateConnectoidNode> createCentroidToConjugateNodeMapping() {
    var mapping = new HashMap<Centroid, ConjugateConnectoidNode>();
    for (ConjugateConnectoidNode conjugateNode : getConjugateConnectoidNodes()) {
      var originalEdge = conjugateNode.getOriginalEdge();
      if(originalEdge!=null) {
        /* not dummy connected to original centroid */
        continue;
      }

      /* found eligible dymmy conjugate, determine to what centroid it maps */
      var conjugateDummyEdge = conjugateNode.getEdges().iterator().next();
      var originalConnectoidEdge = (ConnectoidEdge) conjugateDummyEdge.getOriginalAdjacentEdges().getEarliestNonNull();
      if(originalConnectoidEdge==null) {
        LOGGER.severe(String.format("Conjugate connectoid dummy node's (%s) not connected to original centroid, this shouldn't happen", conjugateNode.getXmlId()));
      }
      /* set mapping */
      mapping.put(originalConnectoidEdge.getCentroidVertex(),conjugateNode);
    }
    return mapping;
  }

}
