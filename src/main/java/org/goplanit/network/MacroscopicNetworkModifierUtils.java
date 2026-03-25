package org.goplanit.network;

/**
 * Utilities to make it easier to use network wide network modifier options
 */
public class MacroscopicNetworkModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of layer modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well
   *
   * @param network to apply to
   */
  public static void updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(MacroscopicNetwork network) {
    NetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(network);
  }
}
