package org.goplanit.demands;

import org.goplanit.demands.modifier.event.handler.SyncXmlIdToIdDemandsEntitiesHandler;
import org.goplanit.graph.directed.modifier.event.handler.SyncXmlIdToIdDirectedGraphEntitiesHandler;

/**
 * Utilities to make it easier to use zoning modifier features combined with listeners to create complex funcionality
 */
public class DemandsModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of demands modifier and Event listeners.
   * Note this also recreates all managed id entities ids as well
   *
   * @param demands to apply to
   */
  public static void syncManagedIdEntitiesContainerXmlIdsToIds(Demands demands) {

    SyncXmlIdToIdDemandsEntitiesHandler syncXmlIdToDemandsEntitiesIds = new SyncXmlIdToIdDemandsEntitiesHandler();

    demands.getDemandsModifier().addListener(syncXmlIdToDemandsEntitiesIds);
    demands.getDemandsModifier().recreateManagedEntitiesIds(); // sync ids and XML ids
    demands.getDemandsModifier().removeListener(syncXmlIdToDemandsEntitiesIds);
  }
}
