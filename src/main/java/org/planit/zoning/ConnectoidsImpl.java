package org.planit.zoning;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.djutils.event.EventInterface;
import org.djutils.event.EventListenerInterface;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Connectoids;
import org.planit.zoning.modifier.event.ZoningEvent;

/**
 * Base implementation of Connectoids container and factory class
 * 
 * @author markr
 *
 */
public abstract class ConnectoidsImpl<T extends Connectoid> extends ManagedIdEntitiesImpl<T> implements Connectoids<T>, EventListenerInterface {

  /**
   * generated UID
   */
  private static final long serialVersionUID = -7710154947041263497L;

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
   * @param groupId      to use for creating ids for instances
   * @param parentZoning
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
   * Support event callbacks that require changes on underlying connectoids
   */
  @Override
  public void notify(EventInterface event) throws RemoteException {
    org.djutils.event.EventType eventType = event.getType();

    /* update connectoid zone id references when zone ids have changed */
    if (eventType.equals(ZoningEvent.MODIFIED_ZONE_IDS)) {
      updateConnectoidAccessZoneIdReferences();
    }
  }

}
