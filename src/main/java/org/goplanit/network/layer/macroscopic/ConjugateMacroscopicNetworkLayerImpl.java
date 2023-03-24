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

/**
 * Conjugate of macroscopic physical Network (layer), i.e. the edge-to-vertex dual of its original form
 *
 * @author markr
 *
 */
public class ConjugateMacroscopicNetworkLayerImpl extends UntypedNetworkLayerImpl<ConjugateNode, ConjugateLink, ConjugateLinkSegment> implements ConjugateMacroscopicNetworkLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateMacroscopicNetworkLayerImpl.class.getCanonicalName());

  /** original layer this conjugate layer is based on */
  protected final MacroscopicNetworkLayer originalLayer;

  /**
   * Reset and re-populate entire conjugate network layer based on current state of original layer this is the conjugate of
   * 
   * @param conjugateVirtualNetwork to use when connecting to original connectoid edges/segments
   */
  protected void update(ConjugateVirtualNetwork conjugateVirtualNetwork) {
    reset();

    /* link -> conjugate node */
    Map<DirectedEdge, ConjugateDirectedVertex> edgeToConjugateNode = new HashMap<>();
    for (Link link : originalLayer.getLinks()) {
      ConjugateNode conjugateNode = getConjugateNodes().getFactory().registerNew(link);
      edgeToConjugateNode.put(link, conjugateNode);
    }

    /* also allow for connectoids to be available and connected to newly created conjugate links */
    if (conjugateVirtualNetwork != null) {
      for (var conjugateConnectoidNode : conjugateVirtualNetwork.getConjugateConnectoidNodes()) {
        var originalEdge = conjugateConnectoidNode.getOriginalEdge();
        edgeToConjugateNode.put(originalEdge, conjugateConnectoidNode);
      }
    }

    /* (link,link) -> conjugate link + conjugate link segments */
    for (Node node : originalLayer.getNodes()) {

      var linkIter = node.<Link>getLinks().iterator();
      while (linkIter.hasNext()) {
        var link = linkIter.next();
        var nextLinkIter = node.<Link>getLinks().iterator();

        /* move next link iter to first after link iter */
        while (nextLinkIter.hasNext()) {
          if (nextLinkIter.next().equals(link)) {
            break;
          }
        }
        if (linkIter.hasNext() && !nextLinkIter.hasNext()) {
          LOGGER.warning("Unable to find next link while updating conjugate macroscopic network, this shouldn't happen, abort");
          return;
        }

        /* for all remaining next links after current link create combinations (and in both directions for segments) */
        while (nextLinkIter.hasNext()) {
          var nextLink = nextLinkIter.next();

          ConjugateDirectedVertex conjugateVertexA = edgeToConjugateNode.get(link);
          ConjugateDirectedVertex conjugateVertexB = edgeToConjugateNode.get(nextLink);
          if ((conjugateVertexA == null || conjugateVertexB == null) && conjugateVirtualNetwork != null) {
            LOGGER.warning("Unable to obtain conjugate vertex for original link, this shouldn't happen, skip");
            continue;
          }

          /* conjugate link */
          ConjugateLink conjugateLink = getConjugateLinks().getFactory().registerNew(conjugateVertexA, conjugateVertexB, true, link, nextLink);

          /* conjugate link segments for conjugate link */
          boolean directionAb = true;
          var abPair = conjugateLink.getOriginalAdjacentEdgeSegments(directionAb);
          if (abPair.bothNotNull()) {
            getConjugateLinkSegments().getFactory().registerNew(conjugateLink, directionAb, true);
          }
          directionAb = !directionAb;
          var baPair = conjugateLink.getOriginalAdjacentEdgeSegments(directionAb);
          if (baPair.bothNotNull()) {
            getConjugateLinkSegments().getFactory().registerNew(conjugateLink, directionAb, true);
          }
        }
      }
    }
  }

  /**
   * Constructor
   * 
   * @param groupId       contiguous id generation within this group for instances of this class
   * @param originalLayer this conjugate is based on
   */
  protected ConjugateMacroscopicNetworkLayerImpl(final IdGroupingToken groupId, final MacroscopicNetworkLayer originalLayer) {
    // todo: replace links with Macroscopic conjugate links
    this(groupId, new ConjugateNodesImpl(groupId), new ConjugateLinksImpl(groupId), new ConjugateLinkSegmentsImpl(groupId), originalLayer);
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
  public ConjugateLinks getConjugateLinks() {
    return (ConjugateLinks) getDirectedGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegments getConjugateLinkSegments() {
    return (ConjugateLinkSegments) getDirectedGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodes getConjugateNodes() {
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
    return new ConjugateMacroscopicNetworkLayerImpl(this, true, new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    this.resetChildManagedIdEntities();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetChildManagedIdEntities() {
    super.resetChildManagedIdEntities();
    // no others at this point
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer getOriginalLayer() {
    return originalLayer;
  }

}
