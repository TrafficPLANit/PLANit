package org.goplanit.supply.networkloading;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.supply.network.nodemodel.NodeModelComponent;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Network loading traffic component
 *
 * @author markr
 *
 */
public abstract class NetworkLoading extends PlanitComponent<NetworkLoading> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = 6213911562665516698L;

  /**
   * Base constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NetworkLoading(IdGroupingToken groupId) {
    super(groupId, NetworkLoading.class);
  }

  /**
   * Copy Constructor
   * 
   * @param networkLoading to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected NetworkLoading(NetworkLoading networkLoading, boolean deepCopy) {
    super(networkLoading, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract NetworkLoading shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract NetworkLoading deepClone();

}
