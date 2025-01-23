package org.goplanit.network.layer.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.network.layer.physical.ConjugateLinkSegmentsImpl;
import org.goplanit.network.layer.physical.ConjugateLinksImpl;
import org.goplanit.network.layer.physical.ConjugateNodesImpl;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegments;
import org.goplanit.utils.network.layer.physical.ConjugateLinks;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.ConjugateNodes;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetworkLayer;

/**
 * Conjugate of macroscopic physical Network (layer), i.e. the edge-to-vertex dual of its original form
 *
 * @author markr
 *
 */
public class ConjugateMacroscopicNetworkLayerImpl extends
        UntypedNetworkLayerImpl<ConjugateNode, ConjugateLink, ConjugateLinkSegment> implements
        ConjugateMacroscopicNetworkLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateMacroscopicNetworkLayerImpl.class.getCanonicalName());

  /** original layer this conjugate layer is based on */
  protected final MacroscopicNetworkLayer originalLayer;

  /**
   * Check if original network edge combination hosts a potential turn via its direction link segments
   *
   * @param fromEdge to check
   * @param toEdge to check
   * @return true when present, false otherwise
   */
  private boolean hasPotentialDirectionalTurn(DirectedEdge fromEdge, DirectedEdge toEdge) {
    boolean directionalTurnExistsBetweenConjugateVertices = false;
    for(var es : fromEdge.getEdgeSegments()) {
      for (var exitEs : es.getDownstreamVertex().getExitEdgeSegments()) {
        if (toEdge.getEdgeSegments().contains(exitEs)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param originalLayer this conjugate is based on
   */
  protected ConjugateMacroscopicNetworkLayerImpl(
          final IdGroupingToken groupId, final MacroscopicNetworkLayer originalLayer) {
    // todo: replace links with Macroscopic conjugate links
    this(groupId, new ConjugateNodesImpl(groupId), new ConjugateLinksImpl(groupId),
            new ConjugateLinkSegmentsImpl(groupId), originalLayer);
  }

  /**
   * Constructor
   * 
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param conjugateNodes        to use
   * @param conjugateLinks        to use
   * @param conjugateLinkSegments to use
   * @param originalLayer         this conjugate is based on
   */
  protected ConjugateMacroscopicNetworkLayerImpl(final IdGroupingToken groupId, ConjugateNodes conjugateNodes, ConjugateLinks conjugateLinks,
      ConjugateLinkSegments conjugateLinkSegments, final MacroscopicNetworkLayer originalLayer) {
    super(groupId, conjugateNodes, conjugateLinks, conjugateLinkSegments);
    this.originalLayer = originalLayer;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param nodeMapper to use
   * @param linkMapper to use
   * @param linkSegmentMapper to use
   */
  protected ConjugateMacroscopicNetworkLayerImpl(
      ConjugateMacroscopicNetworkLayerImpl other,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<ConjugateNode> nodeMapper,
      GraphEntityDeepCopyMapper<ConjugateLink> linkMapper,
      GraphEntityDeepCopyMapper<ConjugateLinkSegment> linkSegmentMapper) {
    super(other, deepCopy, nodeMapper, linkMapper, linkSegmentMapper);
    this.originalLayer = other.originalLayer;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateFromReferenceLayer(ConjugateVirtualNetworkLayer conjugateVirtualNetworkLayer) {
    reset(conjugateVirtualNetworkLayer==null);

    // sync supported modes
    registerSupportedModes(getOriginalLayer().getSupportedModes());

    // network entities

    final boolean deriveXmlIdFromOriginalEntities = true;
    String xmlIdPostFix = "*";
    /* link -> conjugate node */
    Map<DirectedEdge, ConjugateDirectedVertex> edgeToConjugateNode = new HashMap<>();
    for (Link link : originalLayer.getLinks()) {
      ConjugateNode conjugateNode =
              getNodes().getFactory().registerNew(link, deriveXmlIdFromOriginalEntities, xmlIdPostFix);
      edgeToConjugateNode.put(link, conjugateNode);
    }

    /* also allow for connectoids to be available and connected to newly created conjugate links */
    if (conjugateVirtualNetworkLayer != null && !conjugateVirtualNetworkLayer.isEmpty()) {
      for (var conjugateConnectoidNode : conjugateVirtualNetworkLayer.getVertices()) {
        var originalEdge = conjugateConnectoidNode.getOriginalEdge();
        if(originalEdge != null) {
          edgeToConjugateNode.put(originalEdge, conjugateConnectoidNode);
        }
      }
    }else{
      LOGGER.info("Conjugate virtual network is not available or empty, ignored in " +
              "conjugate macroscopic network layer recreation");
    }

    /* (link,link) -> conjugate link + conjugate link segments */
    for (Node node : originalLayer.getNodes()) {

      var edgeIter = node.getEdges().iterator();
      while (edgeIter.hasNext()) {
        var edge = edgeIter.next();
        var nextEdgeIter = node.getEdges().iterator();

        /* move next link iter to first after link iter */
        while (nextEdgeIter.hasNext()) {
          if (nextEdgeIter.next().equals(edge)) {
            break;
          }
        }
        if (edgeIter.hasNext() && !nextEdgeIter.hasNext()) {
          LOGGER.warning("Unable to find next link while updating conjugate macroscopic network, " +
                  "this shouldn't happen, abort");
          return;
        }

        /* for all remaining next links after current link create combinations (and in both directions for segments) */
        while (nextEdgeIter.hasNext()) {
          var nextEdge = nextEdgeIter.next();

          ConjugateDirectedVertex conjugateVertexA = edgeToConjugateNode.get(edge);
          ConjugateDirectedVertex conjugateVertexB = edgeToConjugateNode.get(nextEdge);
          if ((conjugateVertexA == null || conjugateVertexB == null) && conjugateVirtualNetworkLayer != null) {
            LOGGER.warning("Unable to obtain conjugate vertex for original link, this shouldn't happen, skip");
            continue;
          }

          // only create conjugate link if a turn in either direction exists, otherwise contiguous id generation will
          // be messed up as we'll have links without link segments which makes no practical sense
          boolean directionalTurnExistsBetweenConjugateVertices =
                  hasPotentialDirectionalTurn(edge, nextEdge) || hasPotentialDirectionalTurn(nextEdge, edge);
          if(!directionalTurnExistsBetweenConjugateVertices){
            continue;
          }


          /* conjugate link */
          boolean registerNewEntityOnItsNodes = true;
          ConjugateLink conjugateLink = getLinks().getFactory().registerNew(
                  conjugateVertexA,
                  conjugateVertexB,
                  registerNewEntityOnItsNodes,
                  edge,
                  nextEdge,
                  deriveXmlIdFromOriginalEntities,
                  xmlIdPostFix);

          /* conjugate link segments for conjugate link */
          boolean directionAb = true;
          var abPair = conjugateLink.getOriginalAdjacentEdgeSegments(directionAb);
          if (abPair.bothNotNull()) {
            getLinkSegments().getFactory().registerNew(
                    conjugateLink,
                    directionAb,
                    registerNewEntityOnItsNodes,
                    deriveXmlIdFromOriginalEntities,
                    xmlIdPostFix);
          }
          directionAb = false;
          var baPair = conjugateLink.getOriginalAdjacentEdgeSegments(directionAb);
          if (baPair.bothNotNull()) {
            getLinkSegments().getFactory().registerNew(
                    conjugateLink,
                    directionAb,
                    registerNewEntityOnItsNodes,
                    deriveXmlIdFromOriginalEntities,
                    xmlIdPostFix);
          }
        }
      }
    }
  }


  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    super.logInfo(prefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinks getLinks() {
    return (ConjugateLinks) getDirectedGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegments getLinkSegments() {
    return (ConjugateLinkSegments) getDirectedGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodes getNodes() {
    return (ConjugateNodes) getDirectedGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayerImpl shallowClone() {
    return new ConjugateMacroscopicNetworkLayerImpl(this, false, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayerImpl deepClone() {
    return new ConjugateMacroscopicNetworkLayerImpl(
            this,
            true,
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset(boolean resetManagedIdToken) {
    super.reset(resetManagedIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer getOriginalLayer() {
    return originalLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logConjugateToOriginalMapping() {
    logConjugateVertexToOriginalEdgeMapping();
    logConjugateEdgeToOriginalEdgesMapping();
    logConjugateEdgeSegmentToOriginalSegmentsMapping();
  }

  /**
   * Log mapping between conjugate layer segments and original directed segments (pair of link segments)
   */
  public void logConjugateEdgeSegmentToOriginalSegmentsMapping() {
    var layerPrefix = LoggingUtils.networkLayerPrefix(getId());
    LOGGER.info(String.format(
            "%sLogging conjugate link segments to original directed segment pair mapping",
            layerPrefix));

    for(var conjSegment : getLinkSegments()){
      var originalSegmentPair = conjSegment.getOriginalAdjacentEdgeSegments();
      var originalEntryIds = originalSegmentPair.first() != null ? originalSegmentPair.first().getIdsAsString() : "-";
      var originalExitIds = originalSegmentPair.second() != null ? originalSegmentPair.second().getIdsAsString() : "-";
      LOGGER.info(String.format("%s[upstreamVertex (%s) - downstreamVertex (%s)] conjugate segment" +
                      " (%s) <--> original link segment pair [ (%s) , (%s) ]",
              layerPrefix,
              conjSegment.getUpstreamVertex().getIdsAsString(),
              conjSegment.getDownstreamVertex().getIdsAsString(),
              conjSegment.getIdsAsString(),
              originalEntryIds,
              originalExitIds));
    }
  }

  /**
   * Log mapping between conjugate layer links and original links (pair of links)
   */
  public void logConjugateEdgeToOriginalEdgesMapping() {
    var layerPrefix = LoggingUtils.networkLayerPrefix(getId());
    LOGGER.info(String.format(
            "%sLogging conjugate links to original links pair mapping",
            layerPrefix));

    for(var conjEdge : getLinks()){
      var originalEdgesPair = conjEdge.getOriginalAdjacentEdges();
      var originalEdge1Ids = originalEdgesPair.first() != null ? originalEdgesPair.first().getIdsAsString() : "-";
      var originalEdge2Ids = originalEdgesPair.second() != null ? originalEdgesPair.second().getIdsAsString() : "-";
      LOGGER.info(String.format("%s[vertexA (%s) - vertexB (%s)] conjugate link " +
                      "(%s) <--> original link pair [ (%s) , (%s) ]",
              layerPrefix,
              conjEdge.getVertexA().getIdsAsString(),
              conjEdge.getVertexB().getIdsAsString(),
              conjEdge.getIdsAsString(),
              originalEdge1Ids,
              originalEdge2Ids));
    }
  }

  /**
   * Log mapping between conjugate nodes and original entities (links)
   */
  public void logConjugateVertexToOriginalEdgeMapping() {
    var layerPrefix = LoggingUtils.networkLayerPrefix(getId());
    LOGGER.info(String.format(
            "%sLogging conjugate nodes to original link mapping", layerPrefix));
    for(var conjVertex : getNodes()){
      var originalIds = conjVertex.hasOriginalEdge() ? conjVertex.getOriginalEdge().getIdsAsString() : "-";
      LOGGER.info(String.format("%sconjugate node (%s) <--> original link (%s)",
              layerPrefix, conjVertex.getIdsAsString(), originalIds));
    }
  }

}
