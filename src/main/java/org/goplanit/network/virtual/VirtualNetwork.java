package org.goplanit.network.virtual;

import org.goplanit.network.Network;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConnectoidEdges;
import org.goplanit.utils.network.virtual.ConnectoidSegments;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetwork extends Network {

  // INNER CLASSES

  /** generated id */
  private static final long serialVersionUID = -4088201905917614130L;

  // Protected

  // PUBLIC

  /**
   * Container for connectoid edges
   */
  protected final ConnectoidEdgesImpl connectoidEdges;

  /**
   * Container for connectoid edge segments
   */
  protected final ConnectoidSegmentsImpl connectoidSegments;

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation for instances of this class
   */
  public VirtualNetwork(final IdGroupingToken tokenId) {
    super(tokenId);
    this.connectoidSegments = new ConnectoidSegmentsImpl(getIdGroupingToken());
    this.connectoidEdges = new ConnectoidEdgesImpl(getIdGroupingToken());
  }

  /**
   * Access to connectoid segments
   * 
   * @return connectoidSegments
   */
  public ConnectoidSegments getConnectoidSegments() {
    return connectoidSegments;
  }

  /**
   * Access to connectoid edges
   * 
   * @return connectoidEdges
   */
  public ConnectoidEdges getConnectoidEdges() {
    return connectoidEdges;
  }

  /**
   * free up memory by clearing contents for garbage collection
   */
  public void clear() {
    connectoidEdges.clear();
    connectoidSegments.clear();
  }

}
