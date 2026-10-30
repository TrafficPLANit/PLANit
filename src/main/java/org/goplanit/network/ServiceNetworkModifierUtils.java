package org.goplanit.network;

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
    NetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(serviceNetwork);
  }
}
