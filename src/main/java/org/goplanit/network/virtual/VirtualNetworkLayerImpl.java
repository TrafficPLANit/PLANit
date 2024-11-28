package org.goplanit.network.virtual;

import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.network.virtual.graph.CentroidVerticesImpl;
import org.goplanit.network.virtual.physical.ConnectoidLinksImpl;
import org.goplanit.network.virtual.physical.ConnectoidSegmentsImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.virtual.VirtualNetworkLayer;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.graph.CentroidVertices;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinks;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegments;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Model free virtual network layer which is part of the virtual network and holds all the virtual infrastructure
 * connecting the zones to the physical road network.
 *
 * @author markr
 */
public class VirtualNetworkLayerImpl
        extends UntypedNetworkLayerImpl<CentroidVertex, ConnectoidLink, ConnectoidSegment>
        implements VirtualNetworkLayer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger((VirtualNetworkLayerImpl.class.getCanonicalName()));

  // Protected

  /**
   * Constructor
   *
   * @param tokenId contiguous id generation for instances of this class
   */
  public VirtualNetworkLayerImpl(final IdGroupingToken tokenId) {
    super(tokenId,
            new CentroidVerticesImpl(tokenId),
            new ConnectoidLinksImpl(tokenId),
            new ConnectoidSegmentsImpl(tokenId));
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param connectoidEdgeMapper to use for tracking mapping between original and copied entity (may be null)
   * @param connectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param centroidVertexMapper to use for tracking mapping between original and copied entity (may be null)
   */
  protected VirtualNetworkLayerImpl(
      final VirtualNetworkLayerImpl other,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<ConnectoidLink> connectoidEdgeMapper,
      GraphEntityDeepCopyMapper<ConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<CentroidVertex> centroidVertexMapper) {
    super(other, deepCopy, centroidVertexMapper, connectoidEdgeMapper, connectoidSegmentMapper);

    // replace by calling super now that we use a layer derived from graphs for virtual network. Could be too simplistic
    // so keep below for reference in case we find this goes wrong. To be removed if found to be fine
//    if(deepCopy){
//
//      super.edgeSegments = other.getConnectoidSegments().deepCloneWithMapping(connectoidSegmentMapper);
//      this.connectoidEdges    = other.getConnectoidEdges().deepCloneWithMapping(connectoidEdgeMapper);
//      this.centroidVertices   = other.getCentroidVertices().deepCloneWithMapping(centroidVertexMapper);
//
//      // update edges connected to all centroid vertices as these have been copied and existing references are outdated
//      VertexUtils.updateVertexEdges(centroidVertices, connectoidEdgeMapper::getMapping, true);
//
//      // connectoid edges partly reside in physical network, so we keep those mappings as is, but we do update the centroid vertex mappings
//      EdgeUtils.updateEdgeVertices(
//          connectoidEdges,
//          (DirectedVertex vertex) -> {
//              if( !(vertex instanceof  CentroidVertex)){
//                return null;
//              }
//              return centroidVertexMapper.getMapping((CentroidVertex) vertex);
//          },
//          false);
//      // update connectoid segment parent connectoid edges
//      EdgeSegmentUtils.updateEdgeSegmentParentEdges(connectoidSegments, connectoidEdgeMapper::getMapping, true);
//
//    }else{
//
//      this.connectoidSegments = other.connectoidSegments.shallowClone();
//      this.connectoidEdges    = other.connectoidEdges.shallowClone();
//      this.centroidVertices   = other.centroidVertices.shallowClone();
//
//    }
  }

  @Override
  public ConnectoidLinks getConnectoidLinks() {
    return (ConnectoidLinks) getDirectedGraph().getEdges();
  }

  @Override
  public ConnectoidSegments getConnectoidSegments() {
    return (ConnectoidSegments) getDirectedGraph().getEdgeSegments();
  }

  @Override
  public CentroidVertices getVertices() {
    return (CentroidVertices) getDirectedGraph().getVertices();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#connectoid edges: %d", prefix, getConnectoidLinks().size()));
    LOGGER.info(String.format("%s#connectoid segments: %d", prefix, getConnectoidSegments().size()));
    LOGGER.info(String.format("%s#centroid vertices: %d", prefix, getVertices().size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkLayerImpl createConjugate(IdGroupingToken idToken, boolean resetIdToken) {
    var conjugateLayer = new ConjugateVirtualNetworkLayerImpl(idToken, this);
    conjugateLayer.recreateFromReferenceLayer(resetIdToken);
    return conjugateLayer;
  }

  @Override
  public boolean registerSupportedMode(Mode supportedMode) {
    throw new PlanItRunTimeException("Not yet supported to be derived from physical network for now");
  }

  @Override
  public boolean registerSupportedModes(Collection<Mode> supportedModes) {
    throw new PlanItRunTimeException("Not yet supported to be derived from physical network for now");
  }

  @Override
  public Collection<Mode> getSupportedModes() {
    throw new PlanItRunTimeException("Not yet supported to be derived from physical network for now");
  }

  @Override
  public boolean supportsPredefinedMode(PredefinedModeType predefinedModeType) {
    throw new PlanItRunTimeException("Not yet supported to be derived from physical network for now");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkLayerImpl shallowClone() {
    return new VirtualNetworkLayerImpl(
            this, false, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkLayerImpl deepClone() {
    return deepCloneWithMapping(
            new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkLayerImpl deepCloneWithMapping(GraphEntityDeepCopyMapper<ConnectoidLink> connectoidLinkMapper,
                                                      GraphEntityDeepCopyMapper<ConnectoidSegment> connectoidSegmentMapper,
                                                      GraphEntityDeepCopyMapper<CentroidVertex> centroidVertexMapper) {
    return new VirtualNetworkLayerImpl(
            this,
            true,
        connectoidLinkMapper,
            connectoidSegmentMapper,
            centroidVertexMapper);
  }


}
