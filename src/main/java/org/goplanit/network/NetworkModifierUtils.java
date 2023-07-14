package org.goplanit.network;

import org.goplanit.graph.directed.modifier.event.handler.SyncXmlIdToIdDirectedGraphEntitiesHandler;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;

/**
 * Utilities to make it easier to use network wide network modifier options
 */
class NetworkModifierUtils {

  /**
   * Convenience method to sync XMLids to Ids by making use of layer modifiers and Event listeners.
   * Note this also recreates all managed id entities ids as well
   * @param network to use
   */
  public static <T extends TopologicalLayerNetwork<? extends UntypedDirectedGraphLayer<?,?,?>,?>> void syncManagedIdEntitiesContainerXmlIdsToIds(T network) {

    SyncXmlIdToIdDirectedGraphEntitiesHandler syncXmlIdToNetworkEntitiesIds = new SyncXmlIdToIdDirectedGraphEntitiesHandler();

    /* network layers */
    for(var networkLayer : network.getTransportLayers()){
      var layerModifier = networkLayer.getLayerModifier();

      /* make sure all XML ids are in line with internal ids, so that when we make changes (and sync XML ids to internal ids),
       * we do not accidentally create an XML id that already exists */
      layerModifier.addListener(syncXmlIdToNetworkEntitiesIds);
      layerModifier.recreateManagedIdEntities(); // sync ids and XML ids
      layerModifier.removeListener(syncXmlIdToNetworkEntitiesIds);
    }

    //todo modes not covered yet! --> exclude predefined modes, they are a special case
  }
}
