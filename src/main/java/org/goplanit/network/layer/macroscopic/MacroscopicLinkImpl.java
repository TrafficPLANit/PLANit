package org.goplanit.network.layer.macroscopic;

import org.goplanit.network.layer.physical.LinkImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

import java.util.logging.Logger;

/**
 * Link class connecting two nodes via some geometry. Each link has one or two underlying link segments in a particular direction which may carry additional information for each
 * particular direction of the link.
 *
 * @author markr
 *
 */
public class MacroscopicLinkImpl<N extends DirectedVertex, LS extends MacroscopicLinkSegment> extends LinkImpl<N, LS> implements MacroscopicLink {

  // Protected

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkImpl.class.getCanonicalName());


  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MacroscopicLinkImpl(MacroscopicLinkImpl<N, LS> other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Constructor for empty link. Use with care
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected MacroscopicLinkImpl(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Constructor which injects nodes directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   */
  protected MacroscopicLinkImpl(final IdGroupingToken groupId, final N nodeA, final N nodeB) {
    super(groupId, nodeA, nodeB);
  }

  /**
   * Constructor which injects nodes and link length directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @param length   the length of the link
   */
  protected MacroscopicLinkImpl(final IdGroupingToken groupId, final N nodeA, final N nodeB, final double length) {
    super(groupId, nodeA, nodeB, length);
  }

  // Public

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkImpl<N, LS> shallowClone() {
    return new MacroscopicLinkImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkImpl<N, LS> deepClone() {
    return new MacroscopicLinkImpl<>(this, true);
  }

}
