package org.goplanit.zoning.modifier;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layers.ServiceNetworkLayers;
import org.goplanit.utils.zoning.*;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierListener;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.modifier.event.ModifiedZoneIdsEvent;
import org.goplanit.zoning.modifier.event.RecreatedZoningEntitiesManagedIdsEvent;

/**
 * Implementation of the zoningModifier interface
 * 
 * @author markr
 */
public class ZoningModifierImpl extends EventProducerImpl implements ZoningModifier {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ZoningModifierImpl.class.getCanonicalName());

  /**
   * register listeners for the internally fired events on the internally known containers of the zoning
   */
  private void addInternalEventListeners() {
    this.addListener(zoning.getOdConnectoids());
    this.addListener(zoning.getTransferConnectoids());
    this.addListener(zoning.getTransferZoneGroups());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    ((ZoningModifierListener) eventListener).onZoningModifierEvent((ZoningModificationEvent) event);
  }

  /**
   * The zoning instance to apply modifications on
   */
  protected final Zoning zoning;

  /**
   * remove a zone from its container class (if applicable) without doing anything else
   * 
   * @param toRemove zone to be removed
   */
  protected void removeZone(Zone toRemove) {
    if (toRemove instanceof OdZone) {
      zoning.getOdZones().remove((OdZone) toRemove);
    } else if (toRemove instanceof TransferZone) {
      zoning.getTransferZones().remove((TransferZone) toRemove);
    } else {
      LOGGER.severe(String.format("Unsupported zone %s to be removed by zoning modifier, ignored", Zone.class.getCanonicalName()));
    }
  }

  /**
   * recreate part of zone ids (od zones)
   *
   * @param resetIds flag indicating whether to reset the ids before creating new ones
   */
  protected void recreateOdZoneIds(boolean resetIds) {
    zoning.getOdZones().recreateIds(resetIds);
    fireEvent(new RecreatedZoningEntitiesManagedIdsEvent(this, zoning.getOdZones()));
  }

  /**
   * recreate part of zone ids (transfer zones)
   *
   * @param resetIds flag indicating whether to reset the ids before creating new ones
   */
  protected void recreateTransferZoneIds(boolean resetIds) {
    zoning.getTransferZones().recreateIds(resetIds);
    fireEvent(new RecreatedZoningEntitiesManagedIdsEvent(this, zoning.getTransferZones()));
  }

  /**
   * constructor
   * 
   * @param zoning instance to apply modifications on
   */
  public ZoningModifierImpl(Zoning zoning) {
    this.zoning = zoning;
    addInternalEventListeners();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateConnectoidIds() {
    /*
     * both connectoids containers use the same underlying id generated for the connectoid managed id, so it is unique across the two containers. Hence, we should only reset it
     * once, otherwise it is not longer unique across both when recreating the ids
     */
    boolean recreateManagedIdClass = true;
    zoning.getOdConnectoids().recreateIds(recreateManagedIdClass);
    fireEvent(new RecreatedZoningEntitiesManagedIdsEvent(this, zoning.getOdConnectoids()));

    recreateManagedIdClass = false;
    zoning.getTransferConnectoids().recreateIds(recreateManagedIdClass);
    fireEvent(new RecreatedZoningEntitiesManagedIdsEvent(this, zoning.getTransferConnectoids()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateZoneIds() {
    /*
     * both connectoids containers use the same underlying id generated for the zone managed id, so it is unique across the two containers. Hence, we should only reset it once,
     * otherwise it is no longer unique across both when recreating the ids
     */
    recreateOdZoneIds(true);
    recreateTransferZoneIds(false);

    // used internally by connectoids for example, todo: ideally replace by using the RecreatedZoningEntitiesManagedIdsEvent instead, ugly to have two different events for the same thing
    fireEvent(new ModifiedZoneIdsEvent(this, zoning));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateTransferZoneGroupIds() {
    zoning.getTransferZoneGroups().recreateIds();
    fireEvent(new RecreatedZoningEntitiesManagedIdsEvent(this, zoning.getTransferZoneGroups()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingTransferZones(boolean recreateZoneIds) {
    /* identify all dangling transfer zones */
    Set<Zone> danglingZones = new HashSet<>(zoning.getTransferZones().toCollection());
    zoning.getTransferConnectoids().forEach(connectoid -> danglingZones.removeAll(connectoid.getAccessZones()));

    /* remove all zones that are not referenced by any connectoid */
    if (!danglingZones.isEmpty()) {
      for (Zone danglingZone : danglingZones) {
        removeZone(danglingZone);
        if (danglingZone instanceof TransferZone) {
          ((TransferZone) danglingZone).removeFromAllTransferZoneGroups();
        }
      }

      /* recreate the ids (and their references within the zoning) if any */
      if(recreateZoneIds) {
        recreateZoneIds();
      }

      LOGGER.info(String.format("%sRemoved %d dangling transfer zones", LoggingUtils.zoningPrefix(zoning.getId()), danglingZones.size()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingOdZones(boolean recreateZoneIds) {
    /* identify all dangling OD zones */
    Set<Zone> danglingZones = new HashSet<>(zoning.getOdZones().toCollection());
    zoning.getOdConnectoids().forEach(connectoid -> danglingZones.removeAll(connectoid.getAccessZones()));

    if (!danglingZones.isEmpty()) {
      for (Zone danglingZone : danglingZones) {
        removeZone(danglingZone);

        /* remove all zones that are not referenced by any connectoid */
        if (recreateZoneIds) {
          /* recreate the ids (and their references within the zoning) if any */
          recreateZoneIds();
        }
      }

      LOGGER.info(String.format("%sRemoved %d dangling OD zones", LoggingUtils.zoningPrefix(zoning.getId()), danglingZones.size()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingZones() {
    /* remove all dangling zones, recreate ids only at the end for both (since they share uniqueness of the ids) */
    removeDanglingTransferZones(false);
    removeDanglingOdZones(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingTransferZoneGroups() {

    LongAdder counter = new LongAdder();
    /* remove group if dangling */
    Iterator<TransferZoneGroup> iterator = zoning.getTransferZoneGroups().iterator();
    while (iterator.hasNext()) {
      TransferZoneGroup group = iterator.next();
      if (group.isEmpty()) {
        iterator.remove();
        counter.increment();
      }
    }

    /* recreate the ids if any */
    if (counter.longValue()>0) {
      recreateTransferZoneGroupIds();

      LOGGER.info(String.format("%sRemoved %d dangling transfer zone groups", LoggingUtils.zoningPrefix(zoning.getId()), counter.longValue()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeUnusedTransferConnectoids(final ServiceNetworkLayers serviceNetworkLayers, boolean recreateManagedConnectoidIds) {
    /* partition by physical layer - because a connectoids only relates to a single layer, entries do not occur twice */
    var physicalLayers = serviceNetworkLayers.stream().map(snl -> snl.getParentNetworkLayer()).collect(Collectors.toList());
    var transferConnectoidsByPhysicalLayer =
        this.zoning.getTransferConnectoids().groupByPhysicalLayerAndCustomKey(physicalLayers, DirectedConnectoid::getAccessNode);

    LongAdder counter = new LongAdder();
    for(var serviceNetworkLayer : serviceNetworkLayers){

      var serviceNodesByPhysicalNodes = serviceNetworkLayer.getServiceNodes().groupBy( sn -> sn.getPhysicalParentNodes());
      serviceNodesByPhysicalNodes.remove(null); // make sure that unmapped service nodes do not cause issues

      /* from all entries, remove the entries for which a service node exists --> remaining entries are the ones to remove*/
      var transferConnectoidsByPhysicalAccessNodeToRemove = transferConnectoidsByPhysicalLayer.get(serviceNetworkLayer.getParentNetworkLayer());
      var physicalNodesWithServices = serviceNodesByPhysicalNodes.keySet();
      physicalNodesWithServices.stream().flatMap(e -> e.stream()).forEach(accessNode -> transferConnectoidsByPhysicalAccessNodeToRemove.remove(accessNode)); // prune

      /* remove identified entries from zoning */
      if(transferConnectoidsByPhysicalAccessNodeToRemove!=null && transferConnectoidsByPhysicalAccessNodeToRemove.isEmpty()) {
        transferConnectoidsByPhysicalAccessNodeToRemove.values().stream().flatMap(l -> l.stream()).forEach(
            transferConnectoidToRemove -> zoning.getTransferConnectoids().remove(transferConnectoidToRemove));
        counter.add(transferConnectoidsByPhysicalAccessNodeToRemove.values().stream().collect(Collectors.summingInt(l -> l.size())));
      }
    }

    if(recreateManagedConnectoidIds) {
      recreateConnectoidIds();
    }
    LOGGER.info(String.format("%sRemoved %d unused transfer connectoids", LoggingUtils.zoningPrefix(zoning.getId()), counter.longValue()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedIdEntities() {
    recreateConnectoidIds();
    recreateZoneIds();
    recreateTransferZoneGroupIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(ZoningModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(ZoningModifierListener listener, ZoningModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(ZoningModifierListener listener, ZoningModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(ZoningModifierListener listener) {
    super.removeListener(listener);

  }

}
