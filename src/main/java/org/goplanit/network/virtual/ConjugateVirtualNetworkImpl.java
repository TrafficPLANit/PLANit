package org.goplanit.network.virtual;

import java.util.logging.Logger;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.network.Network;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.physical.BannedMovement;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLink;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegment;

/**
 * Conjugate version (edge-to-vertex-dual) of regular virtual network.
 * 
 * @author markr
 *
 */
public class ConjugateVirtualNetworkImpl extends Network implements ConjugateVirtualNetwork {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateVirtualNetworkImpl.class.getCanonicalName());

  protected ConjugateVirtualNetworkLayerImpl conjugateVirtualLayer;

  /** original virtual network this conjugate is based on */
  protected final VirtualNetwork originalVirtualNetwork;

  /**
   * Reset and re-populate entire conjugate virtual network based on current state of original virtual network
   * this is the conjugate of
   *
   * @param resetManagedIdToken  when true reset token to start ids from zero, otherwise not
   */
  protected void recreateFromReferenceVirtualNetwork(boolean resetManagedIdToken) {
    getLayer().recreateFromReferenceLayer(resetManagedIdToken);
  }

  /**
   * Constructor
   * 
   * @param idToken contiguous id generation for instances of this class
   * @param networkIdGroupingToken token for id generation of managed id classes within network itself
   * @param originalVirtualNetwork to use
   */
  public ConjugateVirtualNetworkImpl(
          IdGroupingToken idToken,
          final IdGroupingToken networkIdGroupingToken,
          final VirtualNetworkImpl originalVirtualNetwork) {
    super(idToken, networkIdGroupingToken);
    // layer is managed by virtual network, so it receives the network id grouping token
    this.conjugateVirtualLayer =
            new ConjugateVirtualNetworkLayerImpl(getNetworkGroupingTokenId(), originalVirtualNetwork.getLayer());
    this.originalVirtualNetwork = originalVirtualNetwork;
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param connectoidEdgeMapper to use for tracking mapping between original and copied entity (may be null)
   * @param connectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param conjugateNodeMapper to use for tracking mapping between original and copied entity (may be null)
   */
  @SuppressWarnings("unchecked")
  protected ConjugateVirtualNetworkImpl(
          final ConjugateVirtualNetworkImpl other,
          boolean deepCopy,
          GraphEntityDeepCopyMapper<? extends ConjugateConnectoidLink> connectoidEdgeMapper,
          GraphEntityDeepCopyMapper<? extends ConjugateConnectoidSegment> connectoidSegmentMapper,
          GraphEntityDeepCopyMapper<? extends ConjugateConnectoidNode> conjugateNodeMapper) {
    super(other, deepCopy);
    this.conjugateVirtualLayer = deepCopy ?
            getLayer().deepCloneWithMapping(
                (GraphEntityDeepCopyMapper<ConjugateConnectoidLink>) connectoidEdgeMapper,
                (GraphEntityDeepCopyMapper<ConjugateConnectoidSegment>) connectoidSegmentMapper,
                (GraphEntityDeepCopyMapper<ConjugateConnectoidNode>) conjugateNodeMapper,
                null) :
            getLayer().shallowClone();

    this.originalVirtualNetwork = other.originalVirtualNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix){
    LOGGER.info(String.format(
        "%s Conjugate virtual network (%s) has %d layer", prefix, getIdsAsString(), 1));
    getLayer().logInfo(prefix + LoggingUtils.virtualNetworkLayerPrefix(getLayer().getId()));
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
  public ConjugateVirtualNetworkLayerImpl getLayer() {
    return conjugateVirtualLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    conjugateVirtualLayer.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    conjugateVirtualLayer.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return getLayer().isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void transform(
          CoordinateReferenceSystem fromCoordinateReferenceSystem,
          CoordinateReferenceSystem toCoordinateReferenceSystem){
    //delegate to single available layer
    getLayer().transform(fromCoordinateReferenceSystem,toCoordinateReferenceSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkImpl shallowClone() {
    return new ConjugateVirtualNetworkImpl(
        this, false, null, null, null );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkImpl deepClone() {
    return deepCloneWithMapping(
        new GraphEntityDeepCopyMapper<>(),
        new GraphEntityDeepCopyMapper<>(),
        new GraphEntityDeepCopyMapper<>(),
        null);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public ConjugateVirtualNetworkImpl deepCloneWithMapping(
      GraphEntityDeepCopyMapper<? extends ConnectoidDirectedEdge> connectoidEdgeMapper,
      GraphEntityDeepCopyMapper<? extends ConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<? extends DirectedVertex> conjugateNodeMapper,
      ManagedIdDeepCopyMapper<BannedMovement> conjugateMovementMapper) {
    // conjugateMovementMapper ignored since conjugate network should have no movements (or at least not supported now)
    return new ConjugateVirtualNetworkImpl(
        this,
        true,
        (GraphEntityDeepCopyMapper<? extends ConjugateConnectoidLink>) connectoidEdgeMapper,
        (GraphEntityDeepCopyMapper<? extends ConjugateConnectoidSegment>) connectoidSegmentMapper,
        (GraphEntityDeepCopyMapper<ConjugateConnectoidNode>)conjugateNodeMapper);
  }

}
