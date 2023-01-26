package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Link class connecting two nodes via some geometry. Each link has one or two underlying link segments in a particular direction which may carry additional information for each
 * particular direction of the link.
 *
 * @author markr
 *
 */
public class LinkImpl<N extends DirectedVertex, LS extends EdgeSegment> extends DirectedEdgeImpl<N, LS> implements Link {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 2360017879557363410L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LinkImpl.class.getCanonicalName());

  /**
   * unique internal identifier
   */
  protected long linkId;

  /**
   * generate unique link id
   *
   * @param tokenId, contiguous id generation within this group for instances of this class
   * @return linkId created
   */
  protected static long generateLinkId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, LINK_ID_CLASS);
  }

  /**
   * Set the link id
   * 
   * @param linkId to set
   */
  protected void setLinkId(long linkId) {
    this.linkId = linkId;
  }

  /**
   * recreate the internal link id and set it
   * 
   * @param tokenId to use
   * @return the created link id
   */
  protected long recreateLinkId(IdGroupingToken tokenId) {
    long newLinkId = generateLinkId(tokenId);
    setLinkId(newLinkId);
    return newLinkId;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected LinkImpl(LinkImpl<N, LS> other, boolean deepCopy) {
    super(other, deepCopy);
    setLinkId(other.getLinkId());
  }

  /**
   * Constructor which injects link length directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   */
  protected LinkImpl(final IdGroupingToken groupId, final N nodeA, final N nodeB) {
    super(groupId, nodeA, nodeB);
    setLinkId(generateLinkId(groupId));
  }

  /**
   * Constructor which injects link length directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @param length   the length of the link
   */
  protected LinkImpl(final IdGroupingToken groupId, final N nodeA, final N nodeB, final double length) {
    super(groupId, nodeA, nodeB, length);
    setLinkId(generateLinkId(groupId));
  }

  // Public

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLinkId() {
    return linkId;
  }

  /**
   * Recreate id and link id
   * 
   * @param tokenId to use
   * @return created id (updated link Id is not returned)
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateLinkId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkImpl<N, LS> clone() {
    return new LinkImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkImpl<N, LS> deepClone() {
    return new LinkImpl<>(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean validate() {
    if (super.validate()) {

      if (getGeometry() != null) {
        if (!getNodeA().getPosition().getCoordinate().equals2D(getGeometry().getCoordinateN(0))) {

          return false;
        }

        if (!getNodeB().getPosition().getCoordinate().equals2D(getGeometry().getCoordinateN(getGeometry().getNumPoints() - 1))) {
          LOGGER.warning(String.format("link (id:%d externalId:%s) geometry inconsistent with extreme node B", getId(), getExternalId()));
          return false;
        }
      }
    }
    return false;
  }

}
