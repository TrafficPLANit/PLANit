package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.TransferZoneGroupFactory;
import org.goplanit.utils.zoning.TransferZoneGroups;

/**
 * Factory for creating transfer zone groups (on container)
 * 
 * @author markr
 */
public class TransferZoneGroupFactoryImpl extends ManagedIdEntityFactoryImpl<TransferZoneGroup> implements TransferZoneGroupFactory {

  /** container to use */
  protected final TransferZoneGroups transferZoneGroups;

  /**
   * Constructor
   * 
   * @param groupId            to use
   * @param transferZoneGroups to use
   */
  protected TransferZoneGroupFactoryImpl(final IdGroupingToken groupId, final TransferZoneGroups transferZoneGroups) {
    super(groupId);
    this.transferZoneGroups = transferZoneGroups;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup registerNew() {
    TransferZoneGroup transferZoneGroup = createNew();
    transferZoneGroups.register(transferZoneGroup);
    return transferZoneGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup createNew() {
    return new TransferZoneGroupImpl(getIdGroupingToken());
  }

}
