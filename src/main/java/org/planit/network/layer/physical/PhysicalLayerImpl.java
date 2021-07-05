package org.planit.network.layer.physical;

import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.LinkSegments;
import org.planit.utils.network.layer.physical.Links;
import org.planit.utils.network.layer.physical.Nodes;
import org.planit.utils.network.layer.physical.PhysicalLayer;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not contain any transport specific information, hence the qualification
 * "model free".
 *
 * @author markr
 */
public class PhysicalLayerImpl extends UntypedPhysicalLayerImpl<Nodes, Links, LinkSegments> implements PhysicalLayer {

  // INNER CLASSES

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PhysicalLayerImpl.class.getCanonicalName());

  // Protected

  // PUBLIC

  /**
   * Network Constructor
   *
   * @param tokenId      contiguous id generation within this group for instances of this class
   * @param nodes        nodes container to use
   * @param links        links container to use
   * @param linkSegments linkSegments container to use
   */
  public PhysicalLayerImpl(final IdGroupingToken tokenId, final Nodes nodes, final Links links, final LinkSegments linkSegments) {
    super(tokenId, nodes, links, linkSegments);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public PhysicalLayerImpl clone() {
    return new PhysicalLayerImpl(getNetworkIdGroupingToken(), (Nodes) nodes.clone(), (Links) links.clone(), (LinkSegments) linkSegments.clone());
  }

}
