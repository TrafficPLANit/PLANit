package org.planit.network.layer.macroscopic;

import java.util.logging.Logger;

import org.planit.network.layer.physical.LinksImpl;
import org.planit.network.layer.physical.NodesImpl;
import org.planit.network.layer.physical.UntypedPhysicalLayerImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.planit.utils.network.layer.physical.Links;
import org.planit.utils.network.layer.physical.Nodes;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;

/**
 * Macroscopic physical Network (layer) that supports one or more modes and link segment types, where the modes are registered on the network (Infrastructure network) level
 *
 * @author markr
 *
 */
public class MacroscopicPhysicalLayerImpl extends UntypedPhysicalLayerImpl<Nodes, Links, MacroscopicLinkSegments>
    implements UntypedPhysicalLayer<Nodes, Links, MacroscopicLinkSegments> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(MacroscopicPhysicalLayerImpl.class.getCanonicalName());

  // Protected

  // Public

  /** The container for the link segment types available across all link segments */
  public final MacroscopicLinkSegmentTypes linkSegmentTypes;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MacroscopicPhysicalLayerImpl(final IdGroupingToken groupId) {
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
  public MacroscopicPhysicalLayerImpl(final IdGroupingToken groupId, Nodes nodes, Links links, MacroscopicLinkSegments linkSegments) {
    super(groupId, nodes, links, linkSegments);
    linkSegmentTypes = new MacroscopicLinkSegmentTypesImpl();
  }

  /**
   * collect the link segment types, alternative to using the public member
   * 
   * @return the link segment types
   */
  public MacroscopicLinkSegmentTypes getLinkSegmentTypes() {
    return this.linkSegmentTypes;
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

}
