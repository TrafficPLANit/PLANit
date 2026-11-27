package org.goplanit.zoning;

import org.goplanit.zoning.modifier.event.handler.SyncXmlIdToIdZoningEntitiesHandler;

/**
 * Utilities to make it easier to use zoning modifier options i.c.w. listener functionality
 */
public class ZoningModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well. Does not cover the virtual network
   *
   * @param zoning to apply to
   */
  public static void updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(Zoning zoning) {
    SyncXmlIdToIdZoningEntitiesHandler syncXmlIdToZoningEntitiesIds = new SyncXmlIdToIdZoningEntitiesHandler();
    zoning.getZoningModifier().addListener(syncXmlIdToZoningEntitiesIds);
    zoning.getZoningModifier().recreateManagedIdEntities();
    zoning.getZoningModifier().removeListener(syncXmlIdToZoningEntitiesIds);
  }
}
