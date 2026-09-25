package org.goplanit.network.virtual.physical;

import org.goplanit.network.layer.physical.LinkImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

import java.util.logging.Logger;

/**
 * Connectoid link implementation that represent links that exist between centroids and connectoids
 * (their node reference), so not physical entities but rather virtual links
 * 
 * @author markr
 *
 */
public class ConnectoidLinkImpl extends LinkImpl<DirectedVertex, ConnectoidSegment> implements ConnectoidLink {

  private static final Logger LOGGER = Logger.getLogger(ConnectoidLinkImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param centroidA the centroidVertex at one end of the connectoid
   * @param vertexB   the vertex at the other end of the connectoid
   * @param length    length of the current connectoid
   */
  protected ConnectoidLinkImpl(
      final IdGroupingToken groupId, final CentroidVertex centroidA, final DirectedVertex vertexB, final double length) {
    super(groupId, centroidA, vertexB, length);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConnectoidLinkImpl(ConnectoidLinkImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinkImpl shallowClone() {
    return new ConnectoidLinkImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinkImpl deepClone() {
    return new ConnectoidLinkImpl(this, true);
  }

}
