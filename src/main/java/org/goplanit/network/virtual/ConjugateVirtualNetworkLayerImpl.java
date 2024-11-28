package org.goplanit.network.virtual;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.UntypedNetworkLayerImpl;
import org.goplanit.network.layer.modifier.UntypedNetworkLayerModifierImpl;
import org.goplanit.network.virtual.physical.conjugate.ConjugateConnectoidLinksImpl;
import org.goplanit.network.virtual.physical.conjugate.ConjugateConnectoidNodesImpl;
import org.goplanit.network.virtual.physical.conjugate.ConjugateConnectoidSegmentsImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.conjugate.*;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Model free conjugate virtual network layer which is part of the conjugate virtual network and holds all the
 * conjugate virtual infrastructure
 *
 * @author markr
 */
public class ConjugateVirtualNetworkLayerImpl
        extends UntypedNetworkLayerImpl<ConjugateConnectoidNode, ConjugateConnectoidLink, ConjugateConnectoidSegment>
        implements ConjugateVirtualNetworkLayer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger((ConjugateVirtualNetworkLayerImpl.class.getCanonicalName()));

  // Protected

  /** the reference layer this conjugate layer is based on */
  protected VirtualNetworkLayerImpl referenceLayer;

  /**
   * Update the layer by syncing it to the current non-conjugate reference layer
   *
   * @param resetManagedIds when true reset the id token such that generated managed ids will start from zero again
   */
  protected void recreateFromReferenceLayer(boolean resetManagedIds) {
    reset(resetManagedIds);

    Map<DirectedVertex, ConjugateConnectoidNode> dummyConjugateNodePerCentroidVertex = new HashMap<>();
    if(referenceLayer.isEmpty()){
      LOGGER.warning("Reference layer of virtual conjugate layer is empty, unable to populate conjugate virtual " +
              "layer, aborting update, consider integrating virtual network with physical network first through " +
              "transport model network");
      return;
    }

    /* connectoid edge -> conjugate connectoid node  + conjugate connectoid link(segments) from dummy to conjugate node*/
    for (var referenceConnectoidEdge : getReferenceLayer().getConnectoidLinks()) {

      var centroid = referenceConnectoidEdge.getCentroidVertex();
      var conjugateDummyNode = dummyConjugateNodePerCentroidVertex.get(centroid);
      if (conjugateDummyNode == null) {
        conjugateDummyNode = getVertices().getFactory().registerNew(null);
        dummyConjugateNodePerCentroidVertex.put(centroid, conjugateDummyNode);
      }
      var conjugateNode = getVertices().getFactory().registerNew(referenceConnectoidEdge);

      /* create "fake" conjugate connectoid edge (where one of the two conjugate connectoid nodes has no original
       * network equivalent but reflects a conjugate centroid) */
      var conjugateLink = this.getConnectoidLinks().getFactory().registerNew(
              conjugateDummyNode, conjugateNode, true, referenceConnectoidEdge);

      // create conjugate connectoid segments between the two nodes to create connectoid turn segments where either
      // the incoming or outgoing original edge segment is null this ensures we can have a generic path search
      // algorithm where we consistently use either incoming or outgoing original edge segment costs
      getConnectoidSegments().getFactory().registerNew(conjugateLink, true /* ab direction */, true);
      getConnectoidSegments().getFactory().registerNew(conjugateLink, false /* ba direction */, true);
    }
  }

  /**
   * Constructor
   *
   * @param tokenId contiguous id generation for instances of this class
   * @param referenceLayer original layer
   */
  public ConjugateVirtualNetworkLayerImpl(final IdGroupingToken tokenId, VirtualNetworkLayerImpl referenceLayer) {
    super(tokenId,
            new ConjugateConnectoidNodesImpl(tokenId),
            new ConjugateConnectoidLinksImpl(tokenId),
            new ConjugateConnectoidSegmentsImpl(tokenId));
    this.referenceLayer = referenceLayer;
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param conjugateConnectoidLinkMapper to use for tracking mapping between original and copied entity (may be null)
   * @param conjugateConnectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param conjugateNodeMapper to use for tracking mapping between original and copied entity (may be null)
   */
  protected ConjugateVirtualNetworkLayerImpl(
      final ConjugateVirtualNetworkLayerImpl other,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<ConjugateConnectoidLink> conjugateConnectoidLinkMapper,
      GraphEntityDeepCopyMapper<ConjugateConnectoidSegment> conjugateConnectoidSegmentMapper,
      GraphEntityDeepCopyMapper<ConjugateConnectoidNode> conjugateNodeMapper) {
    super(other, deepCopy, conjugateNodeMapper, conjugateConnectoidLinkMapper, conjugateConnectoidSegmentMapper);

    this.referenceLayer = other.referenceLayer; // not owned
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset(boolean resetManagedIdToken) {
    getConnectoidSegments().reset(resetManagedIdToken);
    getVertices().reset(resetManagedIdToken);
    this.getConnectoidLinks().reset(resetManagedIdToken);
  }

  @Override
  public ConjugateConnectoidLinks getConnectoidLinks() {
    return (ConjugateConnectoidLinks) getDirectedGraph().getEdges();
  }

  @Override
  public ConjugateConnectoidSegments getConnectoidSegments() {
    return (ConjugateConnectoidSegments) getDirectedGraph().getEdgeSegments();
  }

  @Override
  public ConjugateConnectoidNodes getVertices() {
    return (ConjugateConnectoidNodes) getDirectedGraph().getVertices();
  }

  @Override
  public VirtualNetworkLayer getReferenceLayer() {
    return referenceLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkLayerImpl shallowClone() {
    return new ConjugateVirtualNetworkLayerImpl(this, false, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkLayerImpl deepClone() {
    return deepCloneWithMapping(
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>());
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

  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#conjugate connectoid links: %d", prefix, this.getConnectoidLinks().size()));
    LOGGER.info(String.format("%s#conjugate connectoid segments: %d", prefix, getConnectoidSegments().size()));
    LOGGER.info(String.format("%s#conjugate nodes: %d", prefix, getVertices().size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkLayerImpl deepCloneWithMapping(
          GraphEntityDeepCopyMapper<ConjugateConnectoidLink> connectoidLinkMapper,
          GraphEntityDeepCopyMapper<ConjugateConnectoidSegment> connectoidSegmentMapper,
          GraphEntityDeepCopyMapper<ConjugateConnectoidNode> conjugateNodeMapper) {
    return new ConjugateVirtualNetworkLayerImpl(
            this, true, connectoidLinkMapper, connectoidSegmentMapper, conjugateNodeMapper);
  }


}
