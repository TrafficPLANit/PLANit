package org.goplanit.network;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.layers.UntypedPhysicalNetworkLayers;

import java.util.function.Predicate;

/**
 * A network that comprises physical topological transport network elements, i.e., roads, rail, etc.
 * 
 * @author markr
 * @param <LS> type of layer container
 * @param <L> type of layer
 */
public abstract class UntypedPhysicalNetwork<L extends UntypedPhysicalLayer<?, ?, ?>,
    LS extends UntypedPhysicalNetworkLayers<L>> extends TopologicalLayerNetwork<L, LS> {

  /**
   * generated UID
   */
  private static final long serialVersionUID = 3909555719315844733L;

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public UntypedPhysicalNetwork(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public UntypedPhysicalNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    this(tokenId, null, coordinateReferenceSystem);
  }

  /**
   * Default constructor
   *
   * @param tokenId              to use for id generation
   * @param networkGroupingToken groupIdToken to use for the network managed ids id generation (if null,
   *                             an auto generated token will be created)
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public UntypedPhysicalNetwork(
          IdGroupingToken tokenId,
          IdGroupingToken networkGroupingToken,
          CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId, networkGroupingToken, coordinateReferenceSystem);
  }

  /**
   * Copy constructor.
   *
   * @param other                   to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param modeMapper to use for tracking mapping between original and copied modes
   * @param layerMapper to use for tracking mapping between original and copied layers
   *
   */
  protected UntypedPhysicalNetwork(
      final UntypedPhysicalNetwork<L, LS> other,
      boolean deepCopy,
      ManagedIdDeepCopyMapper<Mode> modeMapper,
      ManagedIdDeepCopyMapper<L> layerMapper) {
    super(other, deepCopy, modeMapper, layerMapper);
  }

  /**
   * remove any dangling subnetworks from the network's layers if they exist and subsequently reorder the
   * internal ids if needed
   *
   */
  public void removeDanglingSubnetworks(){
    removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true, true);
  }

  /**
   * remove any dangling subnetworks from the network's layers if they exist and subsequently reorder the
   * internal ids if needed based on configuration
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param recreateManagedIds when true recreate managed id entities so they are contiguous again
   */
  public void removeDanglingSubnetworks(
          Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, boolean recreateManagedIds) {
    for (L infrastructureLayer : getTransportLayers()) {
      infrastructureLayer.getLayerModifier().removeDanglingSubnetworks(
              belowSize, aboveSize, alwaysKeepLargest, recreateManagedIds);
    }
  }

  /**
   * remove any dangling subnetworks from the network's layers as per
   * {@link #removeDanglingSubnetworks(Integer, Integer, boolean, boolean)}, except that connectivity is judged
   * only across the edge segments accepted by the given criterion rather than across each layer as a whole.
   * <p>
   * A single layer can carry multiple networks that are independent of each other. Judging them as one graph makes
   * any of them appear dangling purely because it cannot be reached from the others, so supplying a criterion
   * allows each to be pruned on its own terms by invoking this once per criterion.
   * </p>
   *
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param recreateManagedIds when true recreate managed id entities so they are contiguous again
   * @param testEdgeSegment when an edge segment tests positive it is considered part of the network being pruned
   */
  public void removeDanglingSubnetworks(
          Integer belowSize,
          Integer aboveSize,
          boolean alwaysKeepLargest,
          boolean recreateManagedIds,
          Predicate<? super EdgeSegment> testEdgeSegment) {
    removeDanglingSubnetworks(
            belowSize, aboveSize, alwaysKeepLargest, recreateManagedIds, testEdgeSegment, Connectivity.WEAK);
  }

  /**
   * remove any dangling subnetworks from the network's layers as per
   * {@link #removeDanglingSubnetworks(Integer, Integer, boolean, boolean, Predicate)}, with control over what
   * being part of the same subnetwork means.
   * <p>
   * Under {@link Connectivity#STRONG} infrastructure that cannot be both entered and left, e.g. a car park served
   * only by a one way road pointing outwards, forms a subnetwork of its own rather than counting as part of the
   * network it hangs off. Whether that should be removed is a modelling decision rather than a correctness one,
   * which is why it is requested explicitly.
   * </p>
   *
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param recreateManagedIds when true recreate managed id entities so they are contiguous again
   * @param testEdgeSegment when an edge segment tests positive it is considered part of the network being pruned
   * @param connectivity what constitutes belonging to the same subnetwork
   */
  public void removeDanglingSubnetworks(
          Integer belowSize,
          Integer aboveSize,
          boolean alwaysKeepLargest,
          boolean recreateManagedIds,
          Predicate<? super EdgeSegment> testEdgeSegment,
          Connectivity connectivity) {
    for (L infrastructureLayer : getTransportLayers()) {
      infrastructureLayer.getLayerModifier().removeDanglingSubnetworks(
              belowSize, aboveSize, alwaysKeepLargest, recreateManagedIds, testEdgeSegment, connectivity);
    }
  }

  /**
   * Recreate the managed ids of all layers and modes
   */
  @Override
  public void recreateManagedIds(){
    getModes().recreateIds(); //todo: not proper just recreates its own ids any dependent other indices will suffer
    getTransportLayers().recreateIds(); // proper delegates internally to layer modifier via events
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedPhysicalNetwork<L, LS> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedPhysicalNetwork<L, LS> deepClone();
}
