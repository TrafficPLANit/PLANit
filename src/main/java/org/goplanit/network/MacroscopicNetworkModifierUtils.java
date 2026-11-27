package org.goplanit.network;

import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.SyncXmlIdToIdIntersectionsHandler;
import org.goplanit.network.layer.macroscopic.tollroad.modifier.event.handler.SyncXmlIdToIdTollRoadsHandler;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierListener;

/**
 * Utilities to make it easier to use network wide network modifier options
 */
public class MacroscopicNetworkModifierUtils {

  /** Constructor to disallow instance creation */
  private MacroscopicNetworkModifierUtils(){}

  /**
   * Convenience method to sync XMLids to Ids by making use of layer modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well, intersections and toll roads included
   *
   * @param network to apply to
   */
  public static void updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(MacroscopicNetwork network) {
    IntersectionModifierListener syncXmlIdToIntersectionIds = new SyncXmlIdToIdIntersectionsHandler();
    TollRoadModifierListener syncXmlIdToTollRoadIds = new SyncXmlIdToIdTollRoadsHandler();
    network.getTransportLayers().forEach(layer -> {
      layer.getLayerModifier().addListener(syncXmlIdToIntersectionIds);
      layer.getLayerModifier().addListener(syncXmlIdToTollRoadIds);
    });
    NetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(network);
    network.getTransportLayers().forEach(layer -> {
      layer.getLayerModifier().removeListener(syncXmlIdToIntersectionIds);
      layer.getLayerModifier().removeListener(syncXmlIdToTollRoadIds);
    });
  }
}
