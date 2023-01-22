package org.goplanit.zoning;

import java.util.logging.Logger;

import org.goplanit.zoning.modifier.event.ModifiedZoneIdsEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.TransferZoneGroupFactory;
import org.goplanit.utils.zoning.TransferZoneGroups;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;

/**
 * Container for transfer zone groups where each transfer zone group logically groups multiple transfer zones together. Practically this can be used to represent public transport
 * stations for example
 * 
 * @author markr
 *
 */
public class TransferZoneGroupsImpl extends ManagedIdEntitiesImpl<TransferZoneGroup> implements TransferZoneGroups {

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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   */
  public TransferZoneGroupsImpl(TransferZoneGroupsImpl other) {
    super(other);
    this.transferZoneGroupFactory =
            new TransferZoneGroupFactoryImpl(other.transferZoneGroupFactory.getIdGroupingToken(), this);
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
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { ModifiedZoneIdsEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onZoningModifierEvent(ZoningModificationEvent event) {
    /* update connectoid zone id references when zone ids have changed */
    if (event.getType().equals(ModifiedZoneIdsEvent.EVENT_TYPE)) {
      recreateTransferZoneGroupsZoneIdMapping();
    }
  }

}
