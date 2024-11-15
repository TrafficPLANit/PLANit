package org.goplanit.network.virtual;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.modifier.UntypedNetworkLayerModifierImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;
import org.goplanit.utils.network.virtual.*;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Model free virtual network layer which is part of the virtual network and holds all the virtual infrastructure
 * connecting the zones to the physical road network.
 *
 * TODO: instead of extending UntypedDirectedGraphImpl, implement its interface and have a member to hide some inherited
 *   methods (getEdges etc.), and then consolidate some methods copied from UntypedNetworkLayerImpl that for now have been
 *   duplicated
 * 
 * @author markr
 */
public class VirtualNetworkLayerImpl implements VirtualNetworkLayer {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger((VirtualNetworkLayerImpl.class.getCanonicalName()));

  // Protected

  protected UntypedDirectedGraphImpl<CentroidVertex, ConnectoidEdge, ConnectoidSegment> theGraph;

  protected String externalId;

  protected String xmlId;

  /**
   * Constructor
   *
   * @param tokenId contiguous id generation for instances of this class
   */
  public VirtualNetworkLayerImpl(final IdGroupingToken tokenId) {
    this.theGraph = new UntypedDirectedGraphImpl<>(
            tokenId,
            new CentroidVerticesImpl(tokenId),
            new ConnectoidEdgesImpl(tokenId),
            new ConnectoidSegmentsImpl(tokenId));
    this.externalId = null;
    this.xmlId = null;
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
      GraphEntityDeepCopyMapper<ConnectoidEdge> connectoidEdgeMapper,
      GraphEntityDeepCopyMapper<ConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<CentroidVertex> centroidVertexMapper) {
    if(deepCopy){
      theGraph = theGraph.smartDeepClone(
              centroidVertexMapper, connectoidEdgeMapper, connectoidSegmentMapper);
    }else{
      theGraph = other.theGraph;
    }

    this.xmlId = other.getXmlId();
    this.externalId = other.getExternalId();

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

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getConnectoidSegments().reset();
    getVertices().reset();
    getConnectoidEdges().reset();
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
  public ConnectoidEdges getConnectoidEdges() {
    return (ConnectoidEdges) theGraph.getEdges();
  }

  @Override
  public ConnectoidSegments getConnectoidSegments() {
    return (ConnectoidSegments) theGraph.getEdgeSegments();
  }

  @Override
  public CentroidVertices getVertices() {
    return (CentroidVertices) theGraph.getVertices();
  }

  @Override
  public IdGroupingToken getLayerIdGroupingToken() {
    return theGraph.getGraphIdGroupingToken();
  }

  @Override
  public UntypedDirectedGraphLayerModifier<CentroidVertex, ConnectoidEdge, ConnectoidSegment> getLayerModifier() {
    return new UntypedNetworkLayerModifierImpl<>(theGraph);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#connectoid edges: %d", prefix, getConnectoidEdges().size()));
    LOGGER.info(String.format("%s#connectoid segments: %d", prefix, getConnectoidSegments().size()));
    LOGGER.info(String.format("%s#centroid vertices: %d", prefix, getVertices().size()));
  }

  @Override
  public boolean validate() {
    return theGraph.validate();
  }

  @Override
  public void transform(
          CoordinateReferenceSystem fromCoordinateReferenceSystem, 
          CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException {
    try {
      theGraph.transformGeometries(PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem));
    } catch (Exception e) {
      PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem);
      throw new PlanItException(String.format(
              "%s error during transformation of virtual network %s CRS", NetworkLayer.createLayerLogPrefix(this), getXmlId()), e);
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
  public ConjugateVirtualNetworkLayerImpl createConjugate(IdGroupingToken idToken) {
    var conjugateLayer = new ConjugateVirtualNetworkLayerImpl(idToken, this);
    conjugateLayer.update();
    return conjugateLayer;
  }

  @Override
  public boolean isEmpty() {
    return theGraph.isEmpty();
  }

  @Override
  public long getId() {
    return theGraph.getId();
  }

  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    return theGraph.recreateManagedIds(tokenId);
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
  public VirtualNetworkLayerImpl deepCloneWithMapping(GraphEntityDeepCopyMapper<ConnectoidEdge> connectoidEdgeMapper,
                                                      GraphEntityDeepCopyMapper<ConnectoidSegment> connectoidSegmentMapper,
                                                      GraphEntityDeepCopyMapper<CentroidVertex> centroidVertexMapper) {
    return new VirtualNetworkLayerImpl(
            this,
            true,
            connectoidEdgeMapper,
            connectoidSegmentMapper,
            centroidVertexMapper);
  }


}
