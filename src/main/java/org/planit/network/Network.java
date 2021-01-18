package org.planit.network;

import java.io.Serializable;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * Base class for any network. A network can be macroscopic or otherwise and in turn can be physical or not, etc. This all depends on its implementation. However in all cases it
 * represents something that allows movement of one or more modes across the locations it represents.
 * 
 * @author markr
 *
 */
public class Network extends TrafficAssignmentComponent<Network> implements Serializable {

  /** generated serial id */
  private static final long serialVersionUID = -1434577945513081778L;

  /** shorthand for creating an infrastructure network */
  public static final String INFRASTUCTURE_NETWORK = InfrastructureNetwork.class.getCanonicalName();

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  public Network(final IdGroupingToken tokenId) {
    super(tokenId, Network.class);
  }
}
