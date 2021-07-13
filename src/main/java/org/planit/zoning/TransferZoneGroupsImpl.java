package org.planit.zoning;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.djutils.event.EventInterface;
import org.djutils.event.EventListenerInterface;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroupFactory;
import org.planit.utils.zoning.TransferZoneGroups;
import org.planit.zoning.modifier.ZoningModifierImpl;

/**
 * Container for transfer zone groups where each transfer zone group logically groups multiple transfer zones together. Practically this can be used to represent public transport
 * stations for example
 * 
 * @author markr
 *
 */
public class TransferZoneGroupsImpl extends ManagedIdEntitiesImpl<TransferZoneGroup> implements TransferZoneGroups, EventListenerInterface {

  /**
   * Generated UID
   */
  private static final long serialVersionUID = 1L;

  /** factory to use */
  private final TransferZoneGroupFactory transferZoneGroupFactory;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConnectoidsImpl.class.getCanonicalName());

  /**
   * Update all transfer zone groups' id mappings for underlying transfer zones since the transfer zone id might have changed
   */
  protected void recreateTransferZoneGroupsZoneIdMapping() {
    for (TransferZoneGroup group : this) {
      if (!(group instanceof TransferZoneGroupImpl)) {
        LOGGER.severe("recreation of transfer zone ids utilises unsupported implementation of TransferZoneGroup interface when attempting to update references");
      }
      ((TransferZoneGroupImpl) group).recreateTransferZoneIdMapping();
    }
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public TransferZoneGroupsImpl(final IdGroupingToken groupId) {
    super(TransferZoneGroup::getId, TransferZoneGroup.TRANSFER_ZONE_GROUP_ID_CLASS);
    this.transferZoneGroupFactory = new TransferZoneGroupFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                  to use for creating ids for instances
   * @param transferZoneGroupFactory the factory to use
   */
  public TransferZoneGroupsImpl(final IdGroupingToken groupId, TransferZoneGroupFactory transferZoneGroupFactory) {
    super(TransferZoneGroup::getId, TransferZoneGroup.TRANSFER_ZONE_GROUP_ID_CLASS);
    this.transferZoneGroupFactory = transferZoneGroupFactory;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TransferZoneGroupsImpl(TransferZoneGroupsImpl other) {
    super(other);
    this.transferZoneGroupFactory = other.transferZoneGroupFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroupFactory getFactory() {
    return transferZoneGroupFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroupsImpl clone() {
    return new TransferZoneGroupsImpl(this);
  }

  /**
   * Support event callbacks that require changes on underlying transfer zone groups
   */
  @Override
  public void notify(EventInterface event) throws RemoteException {
    org.djutils.event.EventType eventType = event.getType();

    /* update connectoid zone id references when zone ids have changed */
    if (eventType.equals(ZoningModifierImpl.MODIFIED_ZONE_IDS)) {
      recreateTransferZoneGroupsZoneIdMapping();
    }
  }

}
