package org.planit.zoning.modifier;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.djutils.event.EventProducer;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.modifier.ZoningModifier;
import org.planit.zoning.ConnectoidsImpl;
import org.planit.zoning.TransferZoneGroupsImpl;
import org.planit.zoning.Zoning;
import org.planit.zoning.modifier.event.ZoningEvent;

/**
 * Implementation of the zoningModifier interface
 * 
 * @author markr
 *
 */
public class ZoningModifierImpl extends EventProducer implements ZoningModifier {

  /**
   * Generated UID
   */
  private static final long serialVersionUID = -7378175296694515636L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ZoningModifierImpl.class.getCanonicalName());

  /**
   * register listeners for the internally fired events on the internally known containers of the zoning
   */
  private void addInternalEventListeners() {
    this.addListener((ConnectoidsImpl<?>) zoning.odConnectoids, ZoningEvent.MODIFIED_ZONE_IDS);
    this.addListener((ConnectoidsImpl<?>) zoning.transferConnectoids, ZoningEvent.MODIFIED_ZONE_IDS);
    this.addListener((TransferZoneGroupsImpl) zoning.transferZoneGroups, ZoningEvent.MODIFIED_ZONE_IDS);
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
    zoning.odConnectoids.recreateIds(recreateManagedIdClass);

    recreateManagedIdClass = false;
    zoning.transferConnectoids.recreateIds(recreateManagedIdClass);
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

    fireEvent(new ZoningEvent(zoning, ZoningEvent.MODIFIED_ZONE_IDS));
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
    zoning.odConnectoids.forEach(connectoid -> danglingZones.removeAll(connectoid.getAccessZones()));
    zoning.transferConnectoids.forEach(connectoid -> danglingZones.removeAll(connectoid.getAccessZones()));

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
  public Serializable getSourceId() {
    return this;
  }

}
