package org.goplanit.network;

import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.layers.UntypedPhysicalNetworkLayers;

/**
 * A network that comprises physical topological transport network elements, i.e., roads, rail, etc.
 * 
 * @author markr
 *
 */
public abstract class UntypedPhysicalNetwork<L extends UntypedPhysicalLayer<?, ?, ?>, LS extends UntypedPhysicalNetworkLayers<L>> extends TopologicalLayerNetwork<L, LS> {

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
    super(tokenId, coordinateReferenceSystem);
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
   * @throws PlanItException thrown if error
   * 
   */
  public void removeDanglingSubnetworks() throws PlanItException {
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
  public abstract UntypedPhysicalNetwork shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedPhysicalNetwork deepClone();
}
