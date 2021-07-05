package org.planit.network.layer.physical;

import java.util.logging.Logger;

import org.planit.graph.directed.DirectedEdgeImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;

/**
 * Link class connecting two nodes via some geometry. Each link has one or two underlying link segments in a particular direction which may carry additional information for each
 * particular direction of the link.
 *
 * @author markr
 *
 */
public class LinkImpl extends DirectedEdgeImpl implements Link {

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
   * @return
   */
  protected long recreateLinkId(IdGroupingToken tokenId) {
    long newLinkId = generateLinkId(tokenId);
    setLinkId(newLinkId);
    return newLinkId;
  }

  /**
   * Copy constructor, geometry is deep copied, see also {@code DirectedEdge} copy constructed
   * 
   * @param linkImpl to copy
   */
  protected LinkImpl(LinkImpl linkImpl) {
    super(linkImpl);
    setLinkId(linkImpl.getLinkId());
  }

  /**
   * Constructor which injects link length directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @throws PlanItException thrown if there is an error
   */
  protected LinkImpl(final IdGroupingToken groupId, final Node nodeA, final Node nodeB) throws PlanItException {
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
   * @throws PlanItException thrown if there is an error
   */
  protected LinkImpl(final IdGroupingToken groupId, final Node nodeA, final Node nodeB, final double length) throws PlanItException {
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
  public LinkImpl clone() {
    return new LinkImpl(this);
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
