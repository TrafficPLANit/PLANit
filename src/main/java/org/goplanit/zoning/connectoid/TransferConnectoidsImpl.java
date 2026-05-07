package org.goplanit.zoning.connectoid;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.TransferConnectoidFactory;
import org.goplanit.utils.zoning.connectoid.TransferConnectoids;

import java.util.function.BiConsumer;

/**
 * Implementation of directed connectoids class
 * 
 * @author markr
 *
 */
public class TransferConnectoidsImpl extends ConnectoidsImpl<TransferConnectoid> implements TransferConnectoids {

  /** factory to use */
  private final TransferConnectoidFactory directedConnectoidFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public TransferConnectoidsImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.directedConnectoidFactory = new TransferConnectoidFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                   to use for creating ids for instances
   * @param directedConnectoidFactory the factory to use
   */
  public TransferConnectoidsImpl(final IdGroupingToken groupId, TransferConnectoidFactory directedConnectoidFactory) {
    super(groupId);
    this.directedConnectoidFactory = directedConnectoidFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public TransferConnectoidsImpl(
      TransferConnectoidsImpl other, boolean deepCopy, BiConsumer<TransferConnectoid, TransferConnectoid> mapper) {
    super(other, deepCopy, mapper);
    this.directedConnectoidFactory =
            new TransferConnectoidFactoryImpl(other.directedConnectoidFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoidFactory getFactory() {
    return directedConnectoidFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional directed connectoid id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), TransferConnectoid.TRANSFER_CONNECTOID_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoidsImpl shallowClone() {
    return new TransferConnectoidsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoidsImpl deepClone() {
    return new TransferConnectoidsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoidsImpl deepCloneWithMapping(BiConsumer<TransferConnectoid, TransferConnectoid> mapper) {
    return new TransferConnectoidsImpl(this, true, mapper);
  }

}
