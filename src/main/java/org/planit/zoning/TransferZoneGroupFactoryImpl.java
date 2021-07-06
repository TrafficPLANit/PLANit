package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroupFactory;
import org.planit.utils.zoning.TransferZoneGroups;

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
   * @param groupId             to use
   * @param directedConnectoids to use
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

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup registerUniqueCopyOf(ManagedId transferZoneGroup) {
    TransferZoneGroup copy = createUniqueCopyOf(transferZoneGroup);
    transferZoneGroups.register(copy);
    return copy;
  }

}
