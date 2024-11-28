package org.goplanit.network;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.layers.UntypedPhysicalNetworkLayers;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A network that comprises physical topological transport network elements, i.e., roads, rail, etc.
 * 
 * @author markr
 *
 */
public abstract class UntypedPhysicalNetwork<L extends UntypedPhysicalLayer<?, ?, ?>, LS extends UntypedPhysicalNetworkLayers<L>>
        extends TopologicalLayerNetwork<L, LS> {

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
  protected UntypedPhysicalNetwork(final UntypedPhysicalNetwork<L, LS> other, boolean deepCopy, ManagedIdDeepCopyMapper<Mode> modeMapper, ManagedIdDeepCopyMapper<L> layerMapper) {
    super(other, deepCopy, modeMapper, layerMapper);
  }

  /**
   * remove any dangling subnetworks from the network's layers if they exist and subsequently reorder the internal ids if needed
   *
   */
  public void removeDanglingSubnetworks(){
    removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   */
  public void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) {
    for (L infrastructureLayer : getTransportLayers()) {
      infrastructureLayer.getLayerModifier().removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest);
    }
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
