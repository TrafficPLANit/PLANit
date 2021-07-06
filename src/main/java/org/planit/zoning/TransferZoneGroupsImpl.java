package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroupFactory;
import org.planit.utils.zoning.TransferZoneGroups;

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

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public TransferZoneGroupsImpl(final IdGroupingToken groupId) {
    super(TransferZoneGroup::getId);
    this.transferZoneGroupFactory = new TransferZoneGroupFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                  to use for creating ids for instances
   * @param transferZoneGroupFactory the factory to use
   */
  public TransferZoneGroupsImpl(final IdGroupingToken groupId, TransferZoneGroupFactory transferZoneGroupFactory) {
    super(TransferZoneGroup::getId);
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

}
