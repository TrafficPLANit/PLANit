package org.goplanit.network.virtual;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
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
 * TODO: instead of extending UntypedDirectedGraphImpl, implement its interface and have a member to hide some inherited
 *   methods (getEdges etc.), and then consolidate some methods copied from UntypedNetworkLayerImpl that for now have been
 *   duplicated
 * 
 * @author markr
 */
public class ConjugateVirtualNetworkLayerImpl implements ConjugateVirtualNetworkLayer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger((ConjugateVirtualNetworkLayerImpl.class.getCanonicalName()));

  // Protected

  protected UntypedDirectedGraphImpl<ConjugateConnectoidNode, ConjugateConnectoidLink, ConjugateConnectoidSegment> theGraph;

  protected String externalId;

  protected String xmlId;

  /** the reference layer this conjugate layer is based on */
  protected VirtualNetworkLayerImpl referenceLayer;

  /**
   * {@inheritDoc}
   */
  public Map<CentroidVertex, ConjugateConnectoidNode> createCentroidToConjugateNodeMapping() {
    var mapping = new HashMap<CentroidVertex, ConjugateConnectoidNode>();
    getVertices().stream().filter(ConjugateDirectedVertex::hasOriginalEdge).forEach(
        cn -> mapping.put(cn.getCentroidVertex(), cn));
    return mapping;
  }

  /**
   * Update the layer by syncing it to the current non-conjugate reference layer
   */
  protected void update() {
    reset();

    Map<DirectedVertex, ConjugateConnectoidNode> dummyConjugateNodePerCentroidVertex = new HashMap<>();

    /* connectoid edge -> conjugate connectoid node */
    for (var connectoidEdge : getReferenceLayer().getConnectoidLinks()) {

      var centroid = connectoidEdge.getCentroidVertex();
      var conjugateDummyNode = dummyConjugateNodePerCentroidVertex.get(centroid);
      if (conjugateDummyNode == null) {
        conjugateDummyNode = getVertices().getFactory().registerNew(null);
        dummyConjugateNodePerCentroidVertex.put(centroid, conjugateDummyNode);
      }
      var conjugateNode = getVertices().getFactory().registerNew(connectoidEdge);

      /* create "fake" conjugate connectoid edge (where one of the two conjugate connectoid nodes has no original
       * network equivalent but reflects a conjugate centroid) */
      var conjugateEdge = this.getConnectoidLinks().getFactory().registerNew(
              conjugateDummyNode, conjugateNode, true, connectoidEdge);

      // create conjugate connectoid segments between the two nodes to create connectoid turn segments where either
      // the incoming or outgoing original edge segment is null this ensures we can have a generic path search
      // algorithm where we consistently use either incoming or outgoing original edge segment costs
      getConnectoidSegments().getFactory().registerNew(conjugateEdge, true /* ab direction */, true);
      getConnectoidSegments().getFactory().registerNew(conjugateEdge, false /* ba direction */, true);
    }
  }

  /**
   * Constructor
   *
   * @param tokenId contiguous id generation for instances of this class
   * @param referenceLayer original layer
   */
  public ConjugateVirtualNetworkLayerImpl(final IdGroupingToken tokenId, VirtualNetworkLayerImpl referenceLayer) {
    theGraph = new UntypedDirectedGraphImpl<>(
            tokenId,
            new ConjugateConnectoidNodesImpl(tokenId),
            new ConjugateConnectoidLinksImpl(tokenId),
            new ConjugateConnectoidSegmentsImpl(tokenId));

    this.externalId = null;
    this.xmlId = null;

    this.referenceLayer = referenceLayer;
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param connectoidLinkMapper to use for tracking mapping between original and copied entity (may be null)
   * @param connectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param conjugateNodeMapper to use for tracking mapping between original and copied entity (may be null)
   */
  protected ConjugateVirtualNetworkLayerImpl(
      final ConjugateVirtualNetworkLayerImpl other,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<ConjugateConnectoidLink> connectoidLinkMapper,
      GraphEntityDeepCopyMapper<ConjugateConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<ConjugateConnectoidNode> conjugateNodeMapper) {
    if(deepCopy){
      //todo: verify this works as expected via super class - not tested
      theGraph = other.theGraph.smartDeepClone(conjugateNodeMapper, connectoidLinkMapper, connectoidSegmentMapper);
    }else{
      theGraph = other.theGraph;
    }

    this.xmlId = other.getXmlId();
    this.externalId = other.getExternalId();

    this.referenceLayer = other.referenceLayer; // not owned
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getConnectoidSegments().reset();
    getVertices().reset();
    this.getConnectoidLinks().reset();
  }

  @Override
  public long getId() {
    return theGraph.getId();
  }

  @Override
  public String getExternalId() {
    return externalId;
  }

  @Override
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  @Override
  public String getXmlId() {
    return xmlId;
  }

  @Override
  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  @Override
  public ConjugateConnectoidLinks getConnectoidLinks() {
    return (ConjugateConnectoidLinks) theGraph.getEdges();
  }

  @Override
  public ConjugateConnectoidSegments getConnectoidSegments() {
    return (ConjugateConnectoidSegments) theGraph.getEdgeSegments();
  }

  @Override
  public ConjugateConnectoidNodes getVertices() {
    return (ConjugateConnectoidNodes) theGraph.getVertices();
  }

  @Override
  public VirtualNetworkLayer getReferenceLayer() {
    return referenceLayer;
  }

  @Override
  public IdGroupingToken getLayerIdGroupingToken() {
    return theGraph.getGraphIdGroupingToken();
  }

  @Override
  public UntypedDirectedGraphLayerModifier<ConjugateConnectoidNode, ConjugateConnectoidLink, ConjugateConnectoidSegment> getLayerModifier() {
    return new UntypedNetworkLayerModifierImpl<>(theGraph);
  }

  @Override
  public void transform(
          CoordinateReferenceSystem fromCoordinateReferenceSystem, CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException {
    try {
      theGraph.transformGeometries(PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem));
    } catch (Exception e) {
      PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem);
      throw new PlanItException(String.format("%s error during transformation of physical network %s CRS", NetworkLayer.createLayerLogPrefix(this), getXmlId()), e);
    }
  }

  @Override
  public Envelope createBoundingBox() {
    if(getVertices().isEmpty()){
      return null;
    }

    Envelope envelope = new Envelope(getVertices().iterator().next().getPosition().getCoordinate());
    getVertices().forEach(v -> envelope.expandToInclude(v.getPosition().getCoordinate()));
    return envelope;
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
  public boolean isEmpty() {
    return theGraph.isEmpty();
  }

  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#conjugate connectoid links: %d", prefix, this.getConnectoidLinks().size()));
    LOGGER.info(String.format("%s#conjugate connectoid segments: %d", prefix, getConnectoidSegments().size()));
    LOGGER.info(String.format("%s#conjugate nodes: %d", prefix, getVertices().size()));
  }

  @Override
  public boolean validate() {
    return theGraph.validate();
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


  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    return theGraph.recreateManagedIds(tokenId);
  }

}
