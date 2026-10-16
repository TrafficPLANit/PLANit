package org.goplanit.demands.discrete;

import org.goplanit.demands.event.SyncXmlIdToIdDemandsEntitiesHandler;
import org.goplanit.demands.event.discrete.SyncXmlIdToIdDiscreteDemandsEntitiesHandler;

/**
 * Utilities to make it easier to use modifier features combined with listeners to create complex functionality
 */
public class DiscreteDemandsModifierUtils {

  /** Dummy constructor */
  private DiscreteDemandsModifierUtils(){}

  /**
   * Convenience method to sync XMLids to Ids by making use of discrete demands modifier and Event listeners.
   * Note this also recreates all managed id entities ids as well
   *
   * @param discreteDemands to apply to
   */
  public static void syncManagedIdEntitiesContainerXmlIdsToIds(DiscreteDemands discreteDemands) {

    var syncXmlIdToDemandsEntitiesIdsHandler = new SyncXmlIdToIdDiscreteDemandsEntitiesHandler();

    discreteDemands.getDiscreteDemandsModifier().addListener(syncXmlIdToDemandsEntitiesIdsHandler);
    discreteDemands.getDiscreteDemandsModifier().recreateManagedEntitiesIds(); // sync ids and XML ids
    discreteDemands.getDiscreteDemandsModifier().removeListener(syncXmlIdToDemandsEntitiesIdsHandler);
  }
}
