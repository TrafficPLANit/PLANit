package org.goplanit.network.virtual;

import org.goplanit.network.Network;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConnectoidEdges;
import org.goplanit.utils.network.virtual.ConnectoidSegments;
import org.goplanit.utils.network.virtual.VirtualNetwork;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetworkImpl extends Network implements VirtualNetwork {

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
  public VirtualNetworkImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    this.connectoidSegments = new ConnectoidSegmentsImpl(getIdGroupingToken());
    this.connectoidEdges = new ConnectoidEdgesImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegments getConnectoidSegments() {
    return connectoidSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdges getConnectoidEdges() {
    return connectoidEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    connectoidEdges.clear();
    connectoidSegments.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    connectoidEdges.reset();
    connectoidSegments.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkImpl createConjugate() {
    var conjugateVirtualNetwork = new ConjugateVirtualNetworkImpl(this);
    conjugateVirtualNetwork.update();
    return conjugateVirtualNetwork;
  }

}
