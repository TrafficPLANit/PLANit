package org.planit.supply.networkloading;

import java.io.Serializable;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * Network loading traffic component
 *
 * @author markr
 *
 */
public abstract class NetworkLoading extends TrafficAssignmentComponent<NetworkLoading> implements Serializable {

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
   */
  protected NetworkLoading(NetworkLoading networkLoading) {
    super(networkLoading);
  }

}
