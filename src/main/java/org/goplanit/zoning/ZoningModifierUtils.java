package org.goplanit.zoning;

import org.goplanit.graph.directed.modifier.event.handler.SyncXmlIdToIdDirectedGraphEntitiesHandler;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.zoning.modifier.event.handler.SyncXmlIdToIdZoningEntitiesHandler;

/**
 * Utilities to make it easier to use zoning modifier options i.c.w. listener functionality
 */
public class ZoningModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well
   *
   * @param zoning
   */
  public static void syncManagedIdEntitiesContainerXmlIdsToIds(Zoning zoning) {
    SyncXmlIdToIdZoningEntitiesHandler syncXmlIdToZoningEntitiesIds = new SyncXmlIdToIdZoningEntitiesHandler();
    zoning.getZoningModifier().addListener(syncXmlIdToZoningEntitiesIds);
    zoning.getZoningModifier().recreateManagedIdEntities();
    zoning.getZoningModifier().removeListener(syncXmlIdToZoningEntitiesIds);
  }
}
