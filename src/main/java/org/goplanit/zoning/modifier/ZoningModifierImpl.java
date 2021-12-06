package org.goplanit.zoning.modifier;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierEventType;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierListener;
import org.goplanit.zoning.ConnectoidsImpl;
import org.goplanit.zoning.TransferZoneGroupsImpl;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.modifier.event.ModifiedZoneIdsEvent;

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
    this.addListener((ConnectoidsImpl<?>) zoning.getOdConnectoids());
    this.addListener((ConnectoidsImpl<?>) zoning.getTransferConnectoids());
    this.addListener((TransferZoneGroupsImpl) zoning.transferZoneGroups);
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
      zoning.odZones.remove((OdZone) toRemove);
    } else if (toRemove instanceof TransferZone) {
      zoning.transferZones.remove((TransferZone) toRemove);
    } else {
      LOGGER.severe(String.format("unsupported zone %s to be removed by zoning modifier, ignored", Zone.class.getCanonicalName()));
    }
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

    recreateManagedIdClass = false;
    zoning.getTransferConnectoids().recreateIds(recreateManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateZoneIds() {
    /*
     * both connectoids containers use the same underlying id generated for the zone managed id, so it is unique across the two containers. Hence, we should only reset it once,
     * otherwise it is not longer unique across both when recreating the ids
     */
    boolean resetManagedIdClass = true;
    zoning.odZones.recreateIds(resetManagedIdClass);
    resetManagedIdClass = false;
    zoning.transferZones.recreateIds(resetManagedIdClass);

    fireEvent(new ModifiedZoneIdsEvent(this, zoning));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateTransferZoneGroupIds() {
    zoning.transferZoneGroups.recreateIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingZones() {
    /* identify all dangling zones */
    Set<Zone> danglingZones = new HashSet<Zone>(zoning.odZones.toCollection());
    danglingZones.addAll(zoning.transferZones.toCollection());
    zoning.getOdConnectoids().forEach(connectoid -> danglingZones.removeAll(connectoid.getAccessZones()));
    zoning.getTransferConnectoids().forEach(connectoid -> danglingZones.removeAll(connectoid.getAccessZones()));

    /* remove all remaining zones that are not referenced by any connectoid */
    if (!danglingZones.isEmpty()) {
      for (Zone danglingZone : danglingZones) {
        removeZone(danglingZone);
        if (danglingZone instanceof TransferZone) {
          ((TransferZone) danglingZone).removeFromAllTransferZoneGroups();
        }
      }

      /* recreate the ids (and their references within the zoning) if any */
      recreateZoneIds();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingTransferZoneGroups() {
    boolean groupRemoved = false;
    /* remove group if dangling */
    Iterator<TransferZoneGroup> iterator = zoning.transferZoneGroups.iterator();
    while (iterator.hasNext()) {
      TransferZoneGroup group = iterator.next();
      if (group.isEmpty()) {
        iterator.remove();
        groupRemoved = true;
      }
    }

    /* recreate the ids if any */
    if (groupRemoved) {
      recreateTransferZoneGroupIds();
    }
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
