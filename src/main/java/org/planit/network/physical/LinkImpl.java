package org.planit.network.physical;

import java.util.logging.Logger;

import org.planit.graph.DirectedEdgeImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;

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
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return linkId
   */
  protected static long generateLinkId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Link.class);
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
