package org.planit.network.physical;

import org.opengis.geometry.coordinate.LineString;
import org.planit.graph.EdgeImpl;
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
public class LinkImpl extends EdgeImpl implements Link {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 2360017879557363410L;

  /**
   * unique internal identifier
   */
  protected final long linkId;

  /**
   * External Id of the physical link
   */
  protected Object externalId;
  
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

  // Public

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
  public LinkImpl(final IdGroupingToken groupId, final Node nodeA, final Node nodeB, final double length) throws PlanItException {
    super(groupId, nodeA, nodeB, length);
    this.linkId = generateLinkId(groupId);
  }

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
  public void setExternalId(final Object externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public Object getExternalId() {
    return externalId;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
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

}
