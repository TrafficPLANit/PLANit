package org.goplanit.network;

import org.goplanit.graph.directed.modifier.event.handler.SyncXmlIdToIdDirectedGraphEntitiesHandler;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;

/**
 * Utilities to make it easier to use service network wide network modifier options
 */
public class ServiceNetworkModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of layer modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well
   *
   * @param serviceNetwork to apply to
   */
  public static void syncManagedIdEntitiesContainerXmlIdsToIds(ServiceNetwork serviceNetwork) {
    NetworkModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(serviceNetwork);
  }
}
