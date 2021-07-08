package org.planit.network;

import java.io.Serializable;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGenerator;
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

  /** a token for this network in particular to ensure unique ids across all entities of the same class instance within this network */
  protected IdGroupingToken networkIdGroupingToken;

  /** shorthand for creating a macroscopic infrastructure network */
  public static final String MACROSCOPIC_NETWORK = MacroscopicNetwork.class.getCanonicalName();

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  public Network(final IdGroupingToken tokenId) {
    super(tokenId, Network.class);
    this.networkIdGroupingToken = IdGenerator.createIdGroupingToken(this, getId());
  }

  /**
   * Copy constructor
   * 
   * @param network to clone
   */
  public Network(Network network) {
    super(network);
  }

  /**
   * collect the grouping token for this network instance
   * 
   * @return id grouping token
   */
  public IdGroupingToken getNetworkGroupingTokenId() {
    return networkIdGroupingToken;
  }

  /**
   * Clone the network
   * 
   * @return cloned network
   */
  @Override
  public Network clone() {
    return new Network(this);
  }
}
