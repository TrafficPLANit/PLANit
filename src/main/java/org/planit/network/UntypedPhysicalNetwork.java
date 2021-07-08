package org.planit.network;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.planit.utils.network.layers.UntypedPhysicalNetworkLayers;

/**
 * A network that comprises physical topological transport network elements, i.e., roads, rail, etc.
 * 
 * @author markr
 *
 */
public abstract class UntypedPhysicalNetwork<L extends UntypedPhysicalLayer<?, ?, ?, ?, ?, ?>, LS extends UntypedPhysicalNetworkLayers<L>> extends TopologicalLayerNetwork<L, LS> {

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
}
