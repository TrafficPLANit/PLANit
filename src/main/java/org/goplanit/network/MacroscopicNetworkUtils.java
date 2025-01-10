package org.goplanit.network;

import org.goplanit.network.layer.macroscopic.MacroscopicGridNetworkLayerGenerator;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;

/**
 * Utilities related to the Macroscopic Network class
 *
 * @author markr
 */
public class MacroscopicNetworkUtils {

  /**
   * Create a macroscopic network instance using the id token provided and in addition generate a simple grid-based network layer for the predefined car mode, where each link is
   * bi-directional and has a single link segment type with access for car (nothing else set). For a more sophisticated grid generator configure the dedicated generator class
   * MacroscopicGridNetworkLayerGenerator by overriding its defaults that are used here.
   *
   * @param tokenId to use
   * @param rows    in the grid
   * @param columns in the grid
   * @return created grid network
   */
  public static MacroscopicNetwork createSimpleGrid(final IdGroupingToken tokenId, int rows, int columns) {
    var network = new MacroscopicNetwork(tokenId);
    var carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    MacroscopicGridNetworkLayerGenerator.create(rows, columns, network.getTransportLayers(), carMode).generate();
    return network;
  }

  /**
   * Based on a given network, generate an id grouping token for the conjugate version of this network
   * embedding information about the original in the description
   *
   * @param network to use as reference
   * @return created token
   */
  public static IdGroupingToken generateDerivedConjugateIdGroupingToken(
          MacroscopicNetwork network) {
    return IdGenerator.createIdGroupingToken(
            "Conjugate for original network (" + network.getIdsAsString()+")");
  }
}
