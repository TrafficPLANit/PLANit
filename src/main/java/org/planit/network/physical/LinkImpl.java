package org.planit.network.physical;

import java.util.logging.Logger;

import org.opengis.geometry.coordinate.LineString;
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
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(LinkImpl.class.getCanonicalName());

  /**
   * unique internal identifier
   */
  protected long linkId;

  /**
   * The line geometry of this link if set
   */
  protected LineString lineGeometry;

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
   * Copy constructor
   * 
   * @param linkImpl to copy
   */
  protected LinkImpl(LinkImpl linkImpl) {
    super(linkImpl);
  }

  /**
   * Constructor which injects link length directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param nodeA    the first node in the link
   * @param nodeB    the second node in the link
   * @param length   the length of the link
   * @param name     the name of the link
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
  public LineString getGeometry() {
    return lineGeometry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setGeometry(LineString lineString) {
    this.lineGeometry = lineString;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Link clone() throws CloneNotSupportedException {
    return new LinkImpl(this);
  }

}
