package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Connectoids;

/**
 * Base implementation of Connectoids container and factory class
 * 
 * @author markr
 *
 */
public abstract class ConnectoidsImpl<T extends Connectoid> extends ManagedIdEntitiesImpl<T> implements Connectoids<T> {

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConnectoidsImpl(final IdGroupingToken groupId) {
    super(Connectoid::getId);
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

}
