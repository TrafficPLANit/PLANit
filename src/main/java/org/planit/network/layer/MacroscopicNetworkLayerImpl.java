package org.planit.network.layer;

import java.util.logging.Logger;

import org.planit.network.layer.macroscopic.MacroscopicLinkSegmentTypesImpl;
import org.planit.network.layer.macroscopic.MacroscopicLinkSegmentsImpl;
import org.planit.network.layer.physical.LinksImpl;
import org.planit.network.layer.physical.NodesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Links;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.Nodes;

/**
 * Macroscopic physical Network (layer) that supports one or more modes and link segment types, where the modes are registered on the network (Infrastructure network) level
 *
 * @author markr
 *
 */
public class MacroscopicNetworkLayerImpl extends UntypedPhysicalLayerImpl<Node, Nodes, Link, Links, MacroscopicLinkSegment, MacroscopicLinkSegments>
    implements MacroscopicNetworkLayer {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetworkLayerImpl.class.getCanonicalName());

  /** The container for the link segment types available across all link segments */
  public final MacroscopicLinkSegmentTypes linkSegmentTypes;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected MacroscopicNetworkLayerImpl(final IdGroupingToken groupId) {
    this(groupId, new NodesImpl(groupId), new LinksImpl(groupId), new MacroscopicLinkSegmentsImpl(groupId));
  }

  /**
   * Constructor
   * 
   * @param groupId      contiguous id generation within this group for instances of this class
   * @param nodes        to use
   * @param links        to use
   * @param linkSegments to use
   */
  protected MacroscopicNetworkLayerImpl(final IdGroupingToken groupId, Nodes nodes, Links links, MacroscopicLinkSegments linkSegments) {
    super(groupId, nodes, links, linkSegments);
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param macroscopicNetworkLayerImpl to copy
   */
  protected MacroscopicNetworkLayerImpl(MacroscopicNetworkLayerImpl macroscopicNetworkLayerImpl) {
    super(macroscopicNetworkLayerImpl);
    this.linkSegmentTypes = macroscopicNetworkLayerImpl.linkSegmentTypes.clone();
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    super.logInfo(prefix);

    LOGGER.info(String.format("%s#link segment types: %d", prefix, linkSegmentTypes.size()));
  }

  /**
   * collect the link segment types, alternative to using the public member
   * 
   * @return the link segment types
   */
  @Override
  public MacroscopicLinkSegmentTypes getLinkSegmentTypes() {
    return this.linkSegmentTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Links getLinks() {
    return getGraph().getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegments getLinkSegments() {
    return getGraph().getEdgeSegments();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Nodes getNodes() {
    return getGraph().getVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerImpl clone() {
    return new MacroscopicNetworkLayerImpl(this);
  }

}
