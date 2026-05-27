package org.goplanit.network.layer.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.network.layer.MovementsImpl;
import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.network.layer.physical.ConjugateLinkSegmentsImpl;
import org.goplanit.network.layer.physical.ConjugateLinksImpl;
import org.goplanit.network.layer.physical.ConjugateNodesImpl;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.*;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetworkLayer;

/**
 * Conjugate of macroscopic physical Network (layer), i.e. the edge-to-vertex dual of its original form
 * <p>
 *   does not yet support conjugate (banned) movements, it is assumed any original (banned) movements resulted in
 *   left out conjugate link(segments) and as such conjugate movements serve no purpose.
 * </p>
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
    this(groupId,
        new ConjugateNodesImpl(groupId),
        new ConjugateLinksImpl(groupId),
        new ConjugateLinkSegmentsImpl(groupId),
        new MovementsImpl(groupId),
        originalLayer);
  }

  /**
   * Constructor
   * 
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param conjugateNodes        to use
   * @param conjugateLinks        to use
   * @param conjugateLinkSegments to use
   * @param movements             to use
   * @param originalLayer         this conjugate is based on
   */
  protected ConjugateMacroscopicNetworkLayerImpl(
      final IdGroupingToken groupId,
      ConjugateNodes conjugateNodes,
      ConjugateLinks conjugateLinks,
      ConjugateLinkSegments conjugateLinkSegments,
      Movements movements,
      final MacroscopicNetworkLayer originalLayer) {
    super(groupId, conjugateNodes, conjugateLinks, conjugateLinkSegments, movements);
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
    super(other, deepCopy, nodeMapper, linkMapper, linkSegmentMapper, null);
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
    boolean disallowUTurns = true;

    final boolean deriveXmlIdFromOriginalEntities = true;
    String xmlIdPostFix = "";
    /* link segment -> conjugate node */
    Map<EdgeSegment, ConjugateDirectedVertex> edgeSegmentToConjugateNode = new HashMap<>();
    for (LinkSegment linkSegment : originalLayer.getLinkSegments()) {
      ConjugateNode conjugateNode =
              getNodes().getFactory().registerNew(linkSegment, deriveXmlIdFromOriginalEntities, xmlIdPostFix);
      edgeSegmentToConjugateNode.put(linkSegment, conjugateNode);
    }

    /* also allow for connectoids to be available and connected to newly created conjugate links */
    if (conjugateVirtualNetworkLayer != null && !conjugateVirtualNetworkLayer.isEmpty()) {
      for (var conjugateConnectoidNode : conjugateVirtualNetworkLayer.getVertices()) {
        var original = conjugateConnectoidNode.getOriginalEdgeSegment();
        if(original != null) {
          edgeSegmentToConjugateNode.put(original, conjugateConnectoidNode);
        }
      }
    }else{
      LOGGER.info("Conjugate virtual network is not available or empty, ignored in " +
              "conjugate macroscopic network layer recreation");
    }

    /* (link,link) -> conjugate link + conjugate link segments */
    for (Node node : originalLayer.getNodes()) {
      for(var entrySegment : node.getEntryEdgeSegments()){
        ConjugateDirectedVertex conjugateVertexUp = edgeSegmentToConjugateNode.get(entrySegment);
        for(var exitSegment : node.getExitLinkSegments()){
          if(disallowUTurns && entrySegment.getOppositeDirectionSegment()==exitSegment){
            continue;
          }

          ConjugateDirectedVertex conjugateVertexDown = edgeSegmentToConjugateNode.get(exitSegment);
          if ((conjugateVertexUp == null || conjugateVertexDown == null) && conjugateVirtualNetworkLayer != null) {
            LOGGER.warning("Unable to obtain conjugate vertices for original turn, this shouldn't happen, skip");
            continue;
          }

          // NOTE: in current setup each conjugate link only ever has one conjugate segment since we created a conjugate
          // node per original link segment, e.g., we disentangled directions fully.
          // todo: it would be more correct if we would have conjugate directed and undirected nodes in the
          //  future instead  of abusing the conjugate nodes as if they are directed.

          /* conjugate link */
          boolean registerNewEntityOnItsNodes = true;
          ConjugateLink conjugateLink = getLinks().getFactory().registerNew(
                  conjugateVertexUp,
                  conjugateVertexDown,
                  registerNewEntityOnItsNodes,
                  conjugateVertexUp.getOriginalEdgeSegment(),
                  conjugateVertexDown.getOriginalEdgeSegment(),
                  deriveXmlIdFromOriginalEntities,
                  xmlIdPostFix);

          /* conjugate link segment for conjugate link */
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
    LOGGER.info(String.format("%s#conjugate links: %d", prefix, this.getLinkSegments().size()));
    LOGGER.info(String.format("%s#conjugate segments: %d", prefix, this.getLinks().size()));
    LOGGER.info(String.format("%s#conjugate nodes: %d", prefix, this.getNodes().size()));
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
  public Movements getMovements() {
    return getDirectedGraph().getMovements();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayerImpl shallowClone() {
    return new ConjugateMacroscopicNetworkLayerImpl(
        this, false, null, null, null);
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
      var originalEdgesPair = conjEdge.getOriginalAdjacentSegments();
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
      var originalIds = conjVertex.hasOriginalEdgeSegment() ? conjVertex.getOriginalEdgeSegment().getIdsAsString() : "-";
      LOGGER.info(String.format("%sconjugate node (%s) <--> original link (%s)",
              layerPrefix, conjVertex.getIdsAsString(), originalIds));
    }
  }

}
