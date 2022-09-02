package org.goplanit.zoning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.utils.event.EventType;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.Connectoids;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.zoning.modifier.event.ModifiedZoneIdsEvent;

/**
 * Base implementation of Connectoids container and factory class
 * 
 * @author markr
 *
 */
public abstract class ConnectoidsImpl<T extends Connectoid> extends ManagedIdEntitiesImpl<T> implements Connectoids<T> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConnectoidsImpl.class.getCanonicalName());

  /**
   * update the references to all access zones for all connectoids
   */
  protected void updateConnectoidAccessZoneIdReferences() {
    for (Connectoid connectoid : this) {
      if (!(connectoid instanceof ConnectoidImpl)) {
        LOGGER.severe("recreation of transfer zone ids utilises unsupported implementation of connectoids interface when attempting to update access zone references");
      }
      ((ConnectoidImpl) connectoid).recreateAccessZoneIdMapping();
    }
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConnectoidsImpl(final IdGroupingToken groupId) {
    super(Connectoid::getId, Connectoid.CONNECTOID_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param connectoidsImpl to copy
   */
  public ConnectoidsImpl(ConnectoidsImpl<T> connectoidsImpl) {
    super(connectoidsImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidsImpl<T> clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { ModifiedZoneIdsEvent.EVENT_TYPE };
  }

  /**
   * Support event callbacks whenever zones have been modified
   */
  @Override
  public void onZoningModifierEvent(ZoningModificationEvent event) {

    /* update connectoid zone id references when zone ids have changed */
    if (event.getType().equals(ModifiedZoneIdsEvent.EVENT_TYPE)) {
      updateConnectoidAccessZoneIdReferences();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Zone, Set<T>> createIndexByAccessZone() {
    HashMap<Zone,Set<T>> indexByAccessZone = new HashMap<>();
    for( var connectoid : this){
      for(var validAccessZone : connectoid){
        indexByAccessZone.putIfAbsent(validAccessZone,new HashSet<>());
        indexByAccessZone.get(validAccessZone).add(connectoid);
      }
    }
    return indexByAccessZone;
  }
}
