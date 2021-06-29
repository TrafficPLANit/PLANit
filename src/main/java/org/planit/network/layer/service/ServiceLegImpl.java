package org.planit.network.layer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.planit.graph.DirectedEdgeImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceNode;

/**
 * A service leg connects two service nodes. Underlying are one or more physical links represented by this single service leg.
 * 
 * @author markr
 *
 */
public class ServiceLegImpl extends DirectedEdgeImpl implements ServiceLeg {

  /** Generated UID */
  private static final long serialVersionUID = 822966574857604397L;

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
   * Sum of the underlying network layer link lengths
   */
  @Override
  public double getLengthKm() {
    return networkLayerLinks.stream().collect(Collectors.summingDouble(link -> link.getLengthKm()));
  }
}
