package org.goplanit.network;

import org.goplanit.graph.directed.modifier.event.handler.SyncXmlIdToIdDirectedGraphEntitiesHandler;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;

/**
 * Utilities to make it easier to use network wide network modifier options
 */
public class MacroscopicNetworkModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of layer modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well
   * @param network
   */
  public static void syncManagedIdEntitiesContainerXmlIdsToIds(MacroscopicNetwork network) {
    NetworkModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(network);
  }
}
