package org.goplanit.network.virtual;

import org.goplanit.network.Network;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConnectoidEdges;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegments;
import org.goplanit.utils.network.virtual.VirtualNetwork;

import java.util.logging.Logger;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetworkImpl extends Network implements VirtualNetwork {

  // INNER CLASSES

  /** generated id */
  private static final long serialVersionUID = -4088201905917614130L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger((VirtualNetworkImpl.class.getCanonicalName()));

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
   * Copy constructor. Beware of shallow copying managed id containers within this instance
   *
   * @param other to clone (shallow copy)
   */
  protected VirtualNetworkImpl(final VirtualNetworkImpl other) {
    super(other);
    this.connectoidSegments = new ConnectoidSegmentsImpl(getIdGroupingToken());
    connectoidSegments.addAll(() -> other.getConnectoidSegments().iterator());
    this.connectoidEdges = new ConnectoidEdgesImpl(getIdGroupingToken());
    connectoidEdges.addAll(() -> other.getConnectoidEdges().iterator());
  }

  /**
   * Beware of cloning due to shallow copying managed id containers within this instance
   */
  @Override
  public Network clone() {
    return new VirtualNetworkImpl(this);
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
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#connectoid edges: %d", prefix, getConnectoidEdges().size()));
    LOGGER.info(String.format("%s#connectoid segments: %d", prefix, getConnectoidSegments().size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkImpl createConjugate(IdGroupingToken idToken) {
    var conjugateVirtualNetwork = new ConjugateVirtualNetworkImpl(idToken, this);
    conjugateVirtualNetwork.update();
    return conjugateVirtualNetwork;
  }

}
