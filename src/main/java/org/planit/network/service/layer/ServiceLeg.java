package org.planit.network.service.layer;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.planit.graph.DirectedEdgeImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;

public class ServiceLeg extends DirectedEdgeImpl {

  /** Generated UID */
  private static final long serialVersionUID = 822966574857604397L;

  /** service leg's underlying links connecting its two service nodes */
  protected final Set<Link> networkLayerLinks;

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @throws PlanItException thrown if there is an error
   */
  protected ServiceLeg(final IdGroupingToken tokenId, final ServiceNode nodeA, final ServiceNode nodeB, final Set<Link> networkLayerLinks) throws PlanItException {
    super(tokenId, nodeA, nodeB, -1);
    this.networkLayerLinks = networkLayerLinks;
  }

  /**
   * Copy Constructor. network layer links are shallow copied.
   * 
   * @param serviceLeg to copy
   */
  protected ServiceLeg(ServiceLeg serviceLeg) {
    super(serviceLeg);
    this.networkLayerLinks = new TreeSet<Link>(serviceLeg.networkLayerLinks);
  }

  /**
   * Sum of the underlying network layer link lengths
   */
  @Override
  public double getLengthKm() {
    return networkLayerLinks.stream().collect(Collectors.summingDouble(link -> link.getLengthKm()));
  }
}
