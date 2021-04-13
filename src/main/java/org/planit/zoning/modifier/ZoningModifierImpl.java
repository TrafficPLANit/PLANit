package org.planit.zoning.modifier;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.Zone;
import org.planit.zoning.Zoning;
import org.planit.zoning.ZoningBuilder;

/**
 * Implementation of the zoningModifier interface
 * 
 * @author markr
 *
 */
public class ZoningModifierImpl implements ZoningModifier {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ZoningModifierImpl.class.getCanonicalName());

  /**
   * The zoning instance to apply modifications on
   */
  protected final Zoning zoning;

  /**
   * The zoning builder instance to use for modifications
   */
  protected final ZoningBuilder zoningBuilder;

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
   * @param zoning        instance to apply modifications on
   * @param zoningBuilder instance to use to make modifications
   */
  public ZoningModifierImpl(Zoning zoning, ZoningBuilder zoningBuilder) {
    this.zoning = zoning;
    this.zoningBuilder = zoningBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateConnectoidIds() {
    zoningBuilder.recreateConnectoidIds(zoning.odConnectoids, zoning.transferConnectoids);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateZoneIds() {
    zoningBuilder.recreateOdZoneIds(zoning.odZones, true /* reset zone ids once... */);
    zoningBuilder.recreateTransferZoneIds(zoning.transferZones, zoning.transferZoneGroups, false /* ...but not again */);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateTransferZoneGroupIds() {
    zoningBuilder.recreateTransferZoneGroupIds(zoning.transferZoneGroups);
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
          Set<TransferZoneGroup> transferzoneGroups = ((TransferZone) danglingZone).getTransferZoneGroups();
          if (transferzoneGroups != null) {
            for (TransferZoneGroup group : transferzoneGroups) {
              /* remove zone from group */
              group.removeTransferZone(((TransferZone) danglingZone));
            }
          }
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

}
