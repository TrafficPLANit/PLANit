package org.goplanit.network.layer.service;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegments;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A service leg segment implementation.
 * 
 * @author markr
 *
 */
public class ServiceLegSegmentImpl extends EdgeSegmentImpl<ServiceLeg> implements ServiceLegSegment {

  /**
   * generated UID
   */
  private static final long serialVersionUID = 407229732625691579L;

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceLegSegmentImpl.class.getCanonicalName());

  /** Service leg's underlying links connecting its two service nodes */
  protected List<LinkSegment> networkLayerLinkSegments;

  /**
   * Constructor with no reference to underlying physical links (to be populated later)
   *
   * @param tokenId     contiguous id generation within this group for instances of this class
   * @param parentLeg   parent leg of segment
   * @param directionAB direction of travel
   */
  protected ServiceLegSegmentImpl(final IdGroupingToken tokenId, final ServiceLeg parentLeg, final boolean directionAB) {
    this(tokenId, parentLeg, directionAB, null);
  }

  /**
   * Constructor
   *
   * @param tokenId     contiguous id generation within this group for instances of this class
   * @param parentLeg   parent leg of segment
   * @param directionAB direction of travel
   * @param networkLayerLinkSegments to use
   */
  protected ServiceLegSegmentImpl(final IdGroupingToken tokenId, final ServiceLeg parentLeg, final boolean directionAB, final List<LinkSegment> networkLayerLinkSegments) {
    super(tokenId, parentLeg, directionAB);
    this.networkLayerLinkSegments = networkLayerLinkSegments;
  }

  /**
   * Copy constructor
   * 
   * @param serviceLegSegment to copy
   */
  protected ServiceLegSegmentImpl(ServiceLegSegmentImpl serviceLegSegment) {
    super(serviceLegSegment);
    this.networkLayerLinkSegments = new ArrayList<>(serviceLegSegment.networkLayerLinkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLeg getParent() {
    return super.getParent();
  }

  /**
   * Sum of the underlying network layer link lengths. If no links are registered 0 is returned
   *
   * @return found length
   */
  @Override
  public double getLengthKm() {
    if (networkLayerLinkSegments == null || networkLayerLinkSegments.isEmpty()) {
      return 0;
    }
    return networkLayerLinkSegments.stream().collect(Collectors.summingDouble(ls -> ls.getParent().getLengthKm()));
  }

  /**
   * @return true when all underlying links have a geometry, false otherwise
   */
  @Override
  public boolean hasGeometry() {
    if (networkLayerLinkSegments == null || networkLayerLinkSegments.isEmpty()) {
      return false;
    } else {
      return networkLayerLinkSegments.stream().allMatch(ls -> ls.getParent().hasGeometry());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<LinkSegment> getPhysicalParentSegments() {
    return this.networkLayerLinkSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPhysicalParentSegments(final List<LinkSegment> networkLayerLinkSegments) {
    this.networkLayerLinkSegments = networkLayerLinkSegments;
  }

  /**
   * Validate based on edge that it is, but also make sure that the references to parent network are consistent, i.e., the service nodes reside on the parent links in the right
   * location as well
   */
  @Override
  public boolean validate() {
    boolean valid = super.validate();
    if (valid && hasPhysicalParentSegments()) {
      var nodeA = isDirectionAb() ? getUpstreamServiceNode() : getDownstreamServiceNode();
      var nodeB = isDirectionAb() ? getDownstreamServiceNode(): getUpstreamServiceNode();
      if (!(getParent().getServiceNodeA().getPhysicalParentNode().equals( isDirectionAb() ? getFirstPhysicalLinkSegment().getUpstreamNode() : getLastPhysicalLinkSegment().getDownstreamNode()))) {
        LOGGER.severe(String.format("Service Node A its parent node (%s) on leg %s does not equate to node A (%s) of the first parent link (%s)",
            getParent().getServiceNodeA().getPhysicalParentNode().getXmlId(), getXmlId(), getFirstPhysicalLinkSegment().getUpstreamNode().getXmlId(), getFirstPhysicalLinkSegment().getXmlId()));
        valid = false;
      }
      if (!(getParent().getServiceNodeB().getPhysicalParentNode().equals(isDirectionAb() ? getLastPhysicalLinkSegment().getDownstreamNode() : getFirstPhysicalLinkSegment().getUpstreamNode()))) {
        LOGGER.severe(String.format("Service Node B its parent node (%s) on leg %s does not equate to node B (%s) of the last parent link (%s)",
            getParent().getServiceNodeB().getPhysicalParentNode().getXmlId(), getXmlId(), getLastPhysicalLinkSegment().getDownstreamNode().getXmlId(), getLastPhysicalLinkSegment().getXmlId()));
        valid = false;
      }
    }
    return true;
  }
}
