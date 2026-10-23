package org.goplanit.network;

import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.SyncXmlIdToIdIntersectionsHandler;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;

/**
 * Utilities to make it easier to use network wide network modifier options
 */
public class MacroscopicNetworkModifierUtils {

  /** Constructor to disallow instance creation */
  private MacroscopicNetworkModifierUtils(){}

  /**
   * Convenience method to sync XMLids to Ids by making use of layer modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well, intersections included
   *
   * @param network to apply to
   */
  public static void updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(MacroscopicNetwork network) {
    IntersectionModifierListener syncXmlIdToIntersectionIds = new SyncXmlIdToIdIntersectionsHandler();
    network.getTransportLayers().forEach(layer -> layer.getLayerModifier().addListener(syncXmlIdToIntersectionIds));
    NetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(network);
    network.getTransportLayers().forEach(layer -> layer.getLayerModifier().removeListener(syncXmlIdToIntersectionIds));
  }
}
