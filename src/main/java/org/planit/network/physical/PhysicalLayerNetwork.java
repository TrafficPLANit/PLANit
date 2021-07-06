package org.planit.network.physical;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.network.TopologicalLayerNetwork;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.PhysicalLayer;
import org.planit.utils.network.physical.PhysicalNetworkLayers;

/**
 * A network that comprises physical topological transport network elements, i.e., roads, rail, etc.
 * 
 * @author markr
 *
 */
public abstract class PhysicalLayerNetwork extends TopologicalLayerNetwork<PhysicalLayer, PhysicalNetworkLayers> {

  /**
   * generated UID
   */
  private static final long serialVersionUID = 3909555719315844733L;

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public PhysicalLayerNetwork(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public PhysicalLayerNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId, coordinateReferenceSystem);
  }
}
