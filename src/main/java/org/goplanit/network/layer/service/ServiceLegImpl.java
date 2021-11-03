package org.goplanit.network.layer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceNode;

/**
 * A service leg connects two service nodes. Underlying are one or more physical links represented by this single service leg.
 * 
 * @author markr
 *
 */
public class ServiceLegImpl extends DirectedEdgeImpl implements ServiceLeg {

  /** Generated UID */
  private static final long serialVersionUID = 822966574857604397L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceLegImpl.class.getCanonicalName());

  /** Service leg's underlying links connecting its two service nodes */
  protected List<Link> networkLayerLinks;

  /**
   * Set the network layer link that make up this leg
   * 
   * @param networkLayerLinks to use
   */
  protected void setNetworkLayerLinks(final List<Link> networkLayerLinks) {
    this.networkLayerLinks = networkLayerLinks;
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   */
  protected ServiceLegImpl(final IdGroupingToken tokenId, final ServiceNode nodeA, final ServiceNode nodeB) {
    this(tokenId, nodeA, nodeB, null);
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   */
  protected ServiceLegImpl(final IdGroupingToken tokenId, final ServiceNode nodeA, final ServiceNode nodeB, final List<Link> networkLayerLinks) {
    super(tokenId, nodeA, nodeB);
    this.networkLayerLinks = networkLayerLinks;
  }

  /**
   * Copy Constructor. network layer links are shallow copied.
   * 
   * @param serviceLeg to copy
   */
  protected ServiceLegImpl(ServiceLegImpl serviceLeg) {
    super(serviceLeg);
    this.networkLayerLinks = new ArrayList<Link>(serviceLeg.networkLayerLinks);
  }

  /**
   * Sum of the underlying network layer link lengths. If no links are registered 0 is returned
   * 
   * @return found length
   */
  @Override
  public double getLengthKm() {
    if (networkLayerLinks == null || networkLayerLinks.isEmpty()) {
      return 0;
    }
    return networkLayerLinks.stream().collect(Collectors.summingDouble(link -> link.getLengthKm()));
  }

  /**
   * @returns true when all underlying links have a geometry, false otherwise
   */
  @Override
  public boolean hasGeometry() {
    if (networkLayerLinks == null || networkLayerLinks.isEmpty()) {
      return false;
    } else {
      return networkLayerLinks.stream().allMatch(link -> link.hasGeometry());
    }
  }

  /**
   * @returns true when geometry is present and all underlying links are in ab direction, false otherwise
   */
  @Override
  public boolean isGeometryInAbDirection() {
    if (hasGeometry()) {
      return networkLayerLinks.stream().allMatch(link -> link.isGeometryInAbDirection());
    }
    return false;
  }

  /**
   * Not allowed on service leg, perform on underlying links instead. Always throws exception
   * 
   * @param transformer to use
   */
  @Override
  public void transformGeometry(MathTransform transformer) throws MismatchedDimensionException, TransformException {
    throw new TransformException("Not allowed to transform geometry on service leg since it holds no geometry. Consider transforming underlying parent links instead");
  }

  /**
   * Create absed on underlying links that have a geometry
   * 
   * @return composite envelope, null if no underlying links, or links have no geometry
   */
  @Override
  public Envelope createEnvelope() {
    if (networkLayerLinks == null || networkLayerLinks.isEmpty()) {
      return null;
    }
    Envelope envelope = null;
    for (Link link : networkLayerLinks) {
      if (link.hasGeometry()) {
        if (envelope == null) {
          envelope = link.createEnvelope();
        } else {
          envelope.expandToInclude(link.createEnvelope());
        }
      }
    }
    return envelope;
  }

  /**
   * Not allowed, collect geometry via underlying links instead
   * 
   * @return null
   */
  @Override
  public LineString getGeometry() {
    return null;
  }

  /**
   * Not allowed, set geometry via underlying links instead
   * 
   * @param lineString to use
   */
  @Override
  public void setGeometry(LineString lineString) {
    LOGGER.warning("Not allowed to set geometry on service leg, do so on underlying links instead");
  }

  /**
   * Not allowed, set length via underlying links instead
   * 
   * @param lengthInKm to use
   */
  @Override
  public void setLengthKm(double lengthInKm) {
    LOGGER.warning("Not allowed to set length on service leg, do so on underlying links instead");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Link> getParentLinks() {
    return this.networkLayerLinks;
  }

  /**
   * Validate based on edge that it is, but also make sure that the references to parent network are consistent, i.e., the service nodes reside on the parent links in the right
   * location as well
   */
  @Override
  public boolean validate() {
    boolean valid = super.validate();
    if (valid) {
      if (!(getServiceNodeA().getParentNode().equals(getFirstParentLink().getNodeA()))) {
        LOGGER.severe(String.format("Service Node A its parent node (%s) on leg %s does not equate to node A (%s) of the first parent link (%s)",
            getServiceNodeA().getParentNode().getXmlId(), getXmlId(), getFirstParentLink().getNodeA().getXmlId(), getFirstParentLink().getXmlId()));
        valid = false;
      }
      if (!(getServiceNodeB().getParentNode().equals(getLastParentLink().getNodeB()))) {
        LOGGER.severe(String.format("Service Node B its parent node (%s) on leg %s does not equate to node B (%s) of the last parent link (%s)",
            getServiceNodeB().getParentNode().getXmlId(), getXmlId(), getLastParentLink().getNodeB().getXmlId(), getLastParentLink().getXmlId()));
        valid = false;
      }
    }
    return true;
  }

}
